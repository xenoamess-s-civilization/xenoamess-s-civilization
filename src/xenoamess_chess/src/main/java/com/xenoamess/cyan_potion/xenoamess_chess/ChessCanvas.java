package com.xenoamess.cyan_potion.xenoamess_chess;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Shared visual layout; the native engine uploads this canvas into its 2D renderer. */
public final class ChessCanvas {
    public static final int WIDTH=1280,HEIGHT=820,BOARD_X=28,BOARD_Y=104,BOARD_WIDTH=880,BOARD_HEIGHT=636;
    private final Color ink=new Color(228,234,235),muted=new Color(145,164,174),gold=new Color(255,208,111);
    private final Color[] teams={new Color(60,180,179),new Color(228,124,98)};
    private final BufferedImage image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB);
    private long revision=Long.MIN_VALUE,uiRevision=Long.MIN_VALUE;
    public record Button(String id,String label,int x,int y,int width,int height) {
        public boolean contains(int px,int py) { return px>=x&&px<x+width&&py>=y&&py<y+height; }
    }
    public List<Button> buttons(ChessSession session) {
        List<Button> result=new ArrayList<>();int i=0;
        for(UnitKind kind:UnitKind.values()) { result.add(new Button("deploy:"+kind.name(),kind.label,944+(i%2)*154,128+(i/2)*47,146,40));i++; }
        String[][] actions={{"select","选择 / 取消"},{"rotate","转向 [R]"},{"produce","生产"},{"repair","维修"},{"sweep","横扫"},{"teleport","中继运输"},{"expand","扩张棋盘"}};
        for(i=0;i<actions.length;i++) result.add(new Button(actions[i][0],actions[i][1],944+(i%2)*154,426+(i/2)*35,146,29));
        result.add(new Button("end_turn","结束回合 [SPACE]",944,575,300,43));
        result.add(new Button("standard","标准新局",944,630,146,32));result.add(new Button("showcase","十种单位展示局",1098,630,146,32));
        result.add(new Button("save","保存对局",944,670,146,30));result.add(new Button("load","读取对局",1098,670,146,30));
        result.add(new Button("commanderA","下局 A: "+leader(session.commanderA),944,712,300,29));
        result.add(new Button("commanderB","下局 B: "+leader(session.commanderB),944,747,300,29));return result;
    }
    private static String leader(String value) { return switch(value) { case "VANGUARD" -> "先锋";case "ARTIFICER" -> "工匠";case "EXPEDITION" -> "远征";default -> "无指挥官"; }; }
    public BufferedImage render(ChessSession session) {
        if(revision==session.game.revision&&uiRevision==session.uiRevision) return image;
        revision=session.game.revision;uiRevision=session.uiRevision;
        Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(14,23,31));g.fillRect(0,0,WIDTH,HEIGHT);g.setColor(new Color(23,35,45));g.fillRoundRect(928,12,338,792,18,18);
        text(g,"XENOAMESS CHESS",28,39,24,ink);text(g,"组合 · 生产 · 解体",29,66,13,muted);
        String turn=session.game.winner()==-1?"第 "+session.game.round()+" 轮    "+(session.game.player()==0?"A":"B")+" 方行动":session.game.winner()==2?"对局结束 · 平局":"对局结束 · "+(session.game.winner()==0?"A":"B")+" 方获胜";
        text(g,turn,445,37,18,gold);
        text(g,"A  行动力 "+session.game.actionPoints(0)+"   清除 "+session.game.score(0)+"/12",445,63,13,teams[0]);
        text(g,"B  行动力 "+session.game.actionPoints(1)+"   清除 "+session.game.score(1)+"/12",680,63,13,teams[1]);
        text(g,"WASD 平移视图    +/- 缩放    方向键移动    R 旋转    ESC 取消",28,90,12,muted);
        g.setClip(BOARD_X,BOARD_Y,BOARD_WIDTH,BOARD_HEIGHT);
        int size=session.cellSize;
        for(int row=0;row<=BOARD_HEIGHT/size;row++) for(int column=0;column<=BOARD_WIDTH/size;column++) {
            ChessGame.Cell cell=new ChessGame.Cell(column+session.cameraX,row+session.cameraY);int x=BOARD_X+column*size,y=BOARD_Y+row*size;
            boolean open=session.game.open(cell);g.setColor(open?((cell.x()+cell.y())%2==0?new Color(32,46,56):new Color(35,50,60)):new Color(18,29,38));g.fillRect(x,y,size,size);
            g.setColor(new Color(51,67,77));g.drawRect(x,y,size,size);
            if(open&&Math.floorMod(cell.x(),16)==0&&Math.floorMod(cell.y(),16)==0) text(g,"["+Math.floorDiv(cell.x(),16)+","+Math.floorDiv(cell.y(),16)+"]",x+3,y+12,10,muted);
        }
        for(var unit:session.game.units()) {
            Color team=teams[unit.owner];Color fill=unit.waiting?team.darker():unit.acted?new Color(team.getRed()*2/3,team.getGreen()*2/3,team.getBlue()*2/3):team;
            for(var cell:unit.cells) {
                int x=BOARD_X+(cell.x()-session.cameraX)*size,y=BOARD_Y+(cell.y()-session.cameraY)*size;
                g.setColor(fill);g.fillRect(x+2,y+2,size-3,size-3);
                if(unit.id==session.selected) { g.setStroke(new BasicStroke(2));g.setColor(gold);g.drawRect(x+2,y+2,size-4,size-4);g.setStroke(new BasicStroke(1)); }
            }
            var first=unit.cells.get(0);int x=BOARD_X+(first.x()-session.cameraX)*size,y=BOARD_Y+(first.y()-session.cameraY)*size;
            text(g,unit.kind.symbol+" "+unit.hp+"/"+unit.kind.durability,x+4,y+size/2+2,Math.max(10,size/3),new Color(9,29,35));
            text(g,unit.waiting?"等待":unit.acted?"已动":"#"+unit.id,x+4,y+size-5,Math.max(9,size/4),new Color(16,41,47));
        }
        if(session.mode.equals("deploy")) {
            for(var cell:session.recipe.cells(session.rotation,session.hover.x(),session.hover.y())) {
                int x=BOARD_X+(cell.x()-session.cameraX)*size,y=BOARD_Y+(cell.y()-session.cameraY)*size;
                g.setColor(new Color(255,208,111,75));g.fillRect(x+1,y+1,size-1,size-1);g.setColor(gold);g.drawRect(x+1,y+1,size-2,size-2);
            }
        }
        g.setClip(null);
        text(g,"单位配方",944,42,19,ink);text(g,"先准备棋盘上的祭品，再点击配方部署",944,65,12,muted);
        text(g,"费用为额外行动力，受伤不降低单位能力",944,86,12,muted);
        for(var button:buttons(session)) {
            boolean chosen=button.id.startsWith("deploy:")&&session.mode.equals("deploy")&&button.id.endsWith(session.recipe.name());
            g.setColor(button.id.equals("end_turn")?teams[session.game.player()].darker():chosen?new Color(91,77,46):new Color(41,57,69));
            g.fillRoundRect(button.x,button.y,button.width,button.height,8,8);
            text(g,button.label,button.x+10,button.y+(button.id.startsWith("deploy:")?17:button.height/2+5),button.id.equals("end_turn")?16:13,chosen?gold:ink);
            if(button.id.startsWith("deploy:")) {
                UnitKind kind=UnitKind.valueOf(button.id.substring(7));text(g,kind.symbol+"  "+kind.cost+" AP · HP "+kind.durability+" · ATK "+kind.damage,button.x+10,button.y+33,10,muted);
            }
        }
        var selected=session.selectedUnit();
        text(g,selected==null?"点击棋盘选中己方单位":selected.kind.label+" #"+selected.id+"   "+selected.hp+"/"+selected.kind.durability,944,388,16,selected==null?muted:gold);
        text(g,selected==null?"部署需要匹配形状、类型和满耐久祭品":selected.waiting?"等待下个己方回合":selected.acted?"本回合已经行动":"本回合可行动一次",944,411,12,muted);
        text(g,"状态："+session.message,28,771,14,gold);
        text(g,"模式 "+session.mode+"   视图 ("+session.cameraX+","+session.cameraY+")   棋盘 "+session.game.boards().size()+" 张   "+(session.runtime().equals("native")?"cyan_potion / OpenGL":"HEADLESS / 无窗口验证"),28,798,12,muted);
        g.dispose();return image;
    }
    private static void text(Graphics2D g,String text,int x,int y,int size,Color color) { g.setFont(new Font("Microsoft YaHei",Font.PLAIN,size));g.setColor(color);g.drawString(text,x,y); }
    public byte[] png(ChessSession session) throws Exception { ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(render(session),"png",bytes);return bytes.toByteArray(); }
}
