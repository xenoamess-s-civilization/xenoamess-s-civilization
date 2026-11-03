package com.xenoamess.cyan_potion.xenoamess_chess;

import com.xenoamess.cyan_potion.base.GameWindow;
import com.xenoamess.cyan_potion.base.game_window_components.controllable_game_window_components.AbstractControllableGameWindowComponent;
import com.xenoamess.cyan_potion.base.io.input.keyboard.KeyboardEvent;
import com.xenoamess.cyan_potion.base.render.Texture;
import com.xenoamess.cyan_potion.base.visual.Picture;
import com.xenoamess.cyan_potion.mcp.AutomationBridge;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/** Native cyan_potion component; model, input and automation run on its draw thread. */
public final class ChessScene extends AbstractControllableGameWindowComponent {
    private final ChessSession session;
    private final AutomationBridge bridge;
    private final Texture texture;
    private final Picture picture=new Picture();
    private final ConcurrentLinkedQueue<Runnable> inputs=new ConcurrentLinkedQueue<>();
    private final ByteBuffer pixels=BufferUtils.createByteBuffer(ChessCanvas.WIDTH*ChessCanvas.HEIGHT*4);
    private long uploadedRevision=Long.MIN_VALUE,uploadedUi=Long.MIN_VALUE;
    private boolean canvasAllocated;

    public ChessScene(GameWindow window) throws Exception {
        this(window,Path.of(System.getProperty("chess.automation.session","src/xenoamess_chess/target/automation-session.json")));
    }

    public ChessScene(GameWindow window,Path sessionFile) throws Exception {
        super(window);init(0,0,ChessCanvas.WIDTH,ChessCanvas.HEIGHT);
        glfwSetWindowTitle(window.getWindow(),"XenoAmess Chess");
        session=new ChessSession(Path.of("src/xenoamess_chess/target/saves"));
        texture=getResourceManager().fetchResource(Texture.class,Texture.STRING_PURE_COLOR,"chess-canvas","1,1,1,1");
        picture.setBindable(texture);picture.setLeftTopPosX(0);picture.setLeftTopPosY(0);picture.setWidth(ChessCanvas.WIDTH);picture.setHeight(ChessCanvas.HEIGHT);
        session.nativeCapture(this::captureFrame);
        bridge=new AutomationBridge(session,sessionFile,0);
        Runtime.getRuntime().addShutdownHook(new Thread(bridge::close,"chess-automation-shutdown"));
        registerOnMouseButtonLeftDownCallback(event -> {
            int x=(int)event.getMousePosX(),y=(int)event.getMousePosY();inputs.offer(() -> session.userInput(() -> session.click(x,y)));return null;
        });
        registerOnMouseButtonRightDownCallback(event -> { inputs.offer(session::clearSelection);return null; });
        registerProcessor(KeyboardEvent.class,event -> {
            if(event.getAction()==GLFW_PRESS) {
                String key=switch(event.getKey()) {
                    case GLFW_KEY_SPACE -> "SPACE";case GLFW_KEY_ENTER -> "ENTER";case GLFW_KEY_ESCAPE -> "ESCAPE";
                    case GLFW_KEY_UP -> "UP";case GLFW_KEY_DOWN -> "DOWN";case GLFW_KEY_LEFT -> "LEFT";case GLFW_KEY_RIGHT -> "RIGHT";
                    case GLFW_KEY_W -> "W";case GLFW_KEY_A -> "A";case GLFW_KEY_S -> "S";case GLFW_KEY_D -> "D";case GLFW_KEY_R -> "R";
                    case GLFW_KEY_EQUAL -> "+";case GLFW_KEY_MINUS -> "-";default -> "";
                };
                if(!key.isEmpty()) inputs.offer(() -> session.userInput(() -> session.key(key)));
            }
            return null;
        });
    }
    @Override public boolean draw() {
        Runnable input;while((input=inputs.poll())!=null) input.run();
        session.hover((int)getGameWindow().getMousePosX(),(int)getGameWindow().getMousePosY());
        if(!texture.isInMemory()||uploadedRevision!=session.game.revision||uploadedUi!=session.uiRevision) {
            BufferedImage image=session.canvas.render(session);pixels.clear();
            int[] colors=image.getRGB(0,0,ChessCanvas.WIDTH,ChessCanvas.HEIGHT,null,0,ChessCanvas.WIDTH);
            for(int color:colors) pixels.put((byte)(color>>16)).put((byte)(color>>8)).put((byte)color).put((byte)(color>>24));
            pixels.flip();
            if(!canvasAllocated||!texture.isInMemory()) { if(texture.getGlTexture2DInt()>=0) glDeleteTextures(texture.getGlTexture2DInt());texture.bake(ChessCanvas.WIDTH,ChessCanvas.HEIGHT,pixels);canvasAllocated=true; }
            else { glBindTexture(GL_TEXTURE_2D,texture.getGlTexture2DInt());glTexSubImage2D(GL_TEXTURE_2D,0,0,0,ChessCanvas.WIDTH,ChessCanvas.HEIGHT,GL_RGBA,GL_UNSIGNED_BYTE,pixels); }
            uploadedRevision=session.game.revision;uploadedUi=session.uiRevision;
        }
        picture.draw(getGameWindow());bridge.drain();return true;
    }
    private byte[] captureFrame() throws Exception {
        int width=getGameWindow().getRealWindowWidth(),height=getGameWindow().getRealWindowHeight();
        ByteBuffer buffer=BufferUtils.createByteBuffer(width*height*4);
        glReadBuffer(GL_BACK);glPixelStorei(GL_PACK_ALIGNMENT,1);glReadPixels(0,0,width,height,GL_RGBA,GL_UNSIGNED_BYTE,buffer);
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
            int index=((height-1-y)*width+x)*4;
            int color=((buffer.get(index+3)&255)<<24)|((buffer.get(index)&255)<<16)|((buffer.get(index+1)&255)<<8)|(buffer.get(index+2)&255);
            image.setRGB(x,y,color);
        }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);return bytes.toByteArray();
    }
    @Override public void close() { bridge.close();super.close(); }
}
