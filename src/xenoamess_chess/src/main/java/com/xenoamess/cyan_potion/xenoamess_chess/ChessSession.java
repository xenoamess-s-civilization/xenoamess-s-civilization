package com.xenoamess.cyan_potion.xenoamess_chess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xenoamess.cyan_potion.mcp.AutomationBridge;
import com.xenoamess.cyan_potion.mcp.AutomationTarget;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** UI controller and automation target. All callers are serialized by their host. */
public final class ChessSession implements AutomationTarget {
    public final ChessGame game=new ChessGame();
    public final ChessCanvas canvas=new ChessCanvas();
    public long selected=-1, cargo=-1, receiver=-1, uiRevision;
    public int rotation,cameraX,cameraY,cellSize=36;
    public String mode="select",message="选择己方单位，或选择右侧配方部署。",commanderA="NONE",commanderB="NONE";
    public UnitKind recipe=UnitKind.MINION;
    public ChessGame.Cell pivot=new ChessGame.Cell(0,0),hover=new ChessGame.Cell(0,0);
    private final Path saveDirectory;
    private String runtime="headless";
    @FunctionalInterface public interface Capture { byte[] capture() throws Exception; }
    private Capture capture=() -> canvas.png(this);

    public ChessSession(Path saveDirectory) { this.saveDirectory=saveDirectory.toAbsolutePath().normalize(); }
    public String runtime() { return runtime; }
    public void nativeCapture(Capture capture) { this.capture=capture;runtime="native"; }
    public ChessGame.Unit selectedUnit() { return game.units.get(selected); }
    public void hover(int x,int y) { ChessGame.Cell cell=world(x,y);if(!cell.equals(hover)) { hover=cell;uiRevision++; } }
    public ChessGame.Cell world(int x,int y) { return new ChessGame.Cell(Math.floorDiv(x-ChessCanvas.BOARD_X,cellSize)+cameraX,Math.floorDiv(y-ChessCanvas.BOARD_Y,cellSize)+cameraY); }
    public void clearSelection() { selected=-1;cargo=-1;receiver=-1;mode="select";rotation=0;uiRevision++; }

    @Override public JsonNode state() {
        ObjectNode root=AutomationBridge.JSON.createObjectNode().put("engine","cyan_potion").put("runtime",runtime).put("version","0.1.0");
        root.set("game",game.snapshot());
        root.set("ui",AutomationBridge.JSON.valueToTree(Map.of("selectedId",selected,"mode",mode,"recipe",recipe.name(),"rotation",rotation,
                "message",message,"camera",Map.of("x",cameraX,"y",cameraY,"cellSize",cellSize,"screenX",ChessCanvas.BOARD_X,"screenY",ChessCanvas.BOARD_Y),
                "controls",canvas.buttons(this))));return root;
    }
    @Override public List<Command> commands() {
        List<Command> commands=new ArrayList<>();
        commands.add(command("new_game","Start standard or showcase; commanders NONE/VANGUARD/ARTIFICER/EXPEDITION",Map.of("scenario","string","commanderA","string","commanderB","string")));
        commands.add(command("end_turn","End the active player's turn",Map.of()));
        commands.add(command("deploy","Deploy recipe at world anchor with rotation 0..3",Map.of("kind","string","x","integer","y","integer","rotation","integer"),"kind","x","y"));
        commands.add(command("move","Move or rush in one cardinal direction",Map.of("unitId","integer","dx","integer","dy","integer","steps","integer","targetId","integer"),"unitId","dx","dy"));
        commands.add(command("attack","Attack an enemy unit using normal melee or ranged rules",Map.of("unitId","integer","targetId","integer"),"unitId","targetId"));
        commands.add(command("rotate","Rotate movable multi-cell unit around one of its cells",Map.of("unitId","integer","x","integer","y","integer","turns","integer"),"unitId","x","y"));
        commands.add(command("produce","Use a factory action to produce a waiting minion",Map.of("unitId","integer","x","integer","y","integer"),"unitId","x","y"));
        commands.add(command("repair","Repair a neighboring ally",Map.of("unitId","integer","targetId","integer"),"unitId","targetId"));
        commands.add(command("sweep","Sweep beyond a long edge in direction dx/dy",Map.of("unitId","integer","dx","integer","dy","integer"),"unitId","dx","dy"));
        commands.add(command("teleport","Use source relay, cargo and receiver station; destination is cargo anchor",Map.of("unitId","integer","targetId","integer","receiverId","integer","x","integer","y","integer"),"unitId","targetId","receiverId","x","y"));
        commands.add(command("expand","Open an orthogonally adjacent 16x16 board for 3 AP",Map.of("boardX","integer","boardY","integer"),"boardX","boardY"));
        commands.add(command("resign","Resign the current game",Map.of()));
        commands.add(command("save","Save game using a local named slot",Map.of("slot","string")));
        commands.add(command("load","Restore a validated named save slot",Map.of("slot","string")));
        commands.add(command("ui_click","Click logical window coordinates or a discovered controlId",Map.of("x","integer","y","integer","controlId","string")));
        commands.add(command("ui_key","Press a UI key: arrows, WASD, R, SPACE, ESCAPE, +, -",Map.of("key","string"),"key"));
        return commands;
    }
    private static Command command(String name,String description,Map<String,String> properties,String... required) {
        ObjectNode schema=AutomationBridge.JSON.createObjectNode().put("type","object").put("additionalProperties",false);
        ObjectNode fields=schema.putObject("properties");properties.forEach((key,type) -> fields.putObject(key).put("type",type));
        schema.set("required",AutomationBridge.JSON.valueToTree(required));return new Command(name,description,schema);
    }
    private void validateArguments(String name,JsonNode args) {
        Command command=commands().stream().filter(value -> value.name().equals(name)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown command: "+name));
        ChessGame.require(args.isObject(),"arguments must be an object");
        for(JsonNode required:command.inputSchema().path("required")) ChessGame.require(args.has(required.asText()),"Missing argument: "+required.asText());
        args.fieldNames().forEachRemaining(key -> {
            JsonNode field=command.inputSchema().path("properties").path(key);ChessGame.require(!field.isMissingNode(),"Unknown argument: "+key);
            ChessGame.require(field.path("type").asText().equals("integer")?args.get(key).isIntegralNumber():args.get(key).isTextual(),"Invalid argument type: "+key);
        });
    }
    @Override public JsonNode execute(String name,JsonNode args) {
        validateArguments(name,args);
        try {
            switch(name) {
                case "ui_click" -> {
                    if(args.has("controlId")) {
                        ChessCanvas.Button button=canvas.buttons(this).stream().filter(value -> value.id().equals(args.get("controlId").asText())).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown UI control"));
                        click(button.x()+button.width()/2,button.y()+button.height()/2);
                    } else {
                        ChessGame.require(args.has("x")&&args.has("y"),"x and y, or controlId, are required");click(ChessGame.integer(args,"x",0),ChessGame.integer(args,"y",0));
                    }
                }
                case "ui_key" -> key(args.path("key").asText());
                case "save" -> { Files.createDirectories(saveDirectory);AutomationBridge.JSON.writerWithDefaultPrettyPrinter().writeValue(slot(args).toFile(),game.snapshot());message="已保存对局"; }
                case "load" -> { game.restore(AutomationBridge.JSON.readTree(slot(args).toFile()));clearSelection();message="已读取对局"; }
                default -> {
                    game.execute(name,args);message=game.lastAction();
                    if(name.equals("new_game")) { clearSelection();cameraX=0;cameraY=0;commanderA=game.commander(0);commanderB=game.commander(1); }
                    if(name.equals("end_turn")||name.equals("resign")) clearSelection();
                }
            }
            uiRevision++;return state();
        } catch(RuntimeException failure) { throw failure; }
        catch(Exception failure) { throw new IllegalArgumentException("存档操作失败："+failure.getMessage(),failure); }
    }
    private Path slot(JsonNode args) {
        String slot=args.path("slot").asText("quick");ChessGame.require(slot.matches("[A-Za-z0-9_-]{1,40}"),"Invalid save slot");return saveDirectory.resolve(slot+".json");
    }
    public void apply(String name,Object... pairs) {
        ObjectNode args=AutomationBridge.JSON.createObjectNode();for(int i=0;i<pairs.length;i+=2) args.set(pairs[i].toString(),AutomationBridge.JSON.valueToTree(pairs[i+1]));
        execute(name,args);
    }
    public void click(int x,int y) {
        ChessGame.require(x>=0&&x<ChessCanvas.WIDTH&&y>=0&&y<ChessCanvas.HEIGHT,"Click outside the logical window");
        for(var button:canvas.buttons(this)) if(button.contains(x,y)) { control(button.id());return; }
        if(x<ChessCanvas.BOARD_X||x>=ChessCanvas.BOARD_X+ChessCanvas.BOARD_WIDTH||y<ChessCanvas.BOARD_Y||y>=ChessCanvas.BOARD_Y+ChessCanvas.BOARD_HEIGHT) return;
        ChessGame.Cell cell=world(x,y);ChessGame.Unit hit=game.at(cell),unit=selectedUnit();
        switch(mode) {
            case "deploy" -> { apply("deploy","kind",recipe.name(),"x",cell.x(),"y",cell.y(),"rotation",rotation);mode="select"; }
            case "expand" -> { apply("expand","boardX",Math.floorDiv(cell.x(),16),"boardY",Math.floorDiv(cell.y(),16));mode="select"; }
            case "produce" -> { ChessGame.require(unit!=null,"请先选择工厂");apply("produce","unitId",unit.id,"x",cell.x(),"y",cell.y());mode="select"; }
            case "repair" -> { ChessGame.require(unit!=null&&hit!=null,"请选择待维修友军");apply("repair","unitId",unit.id,"targetId",hit.id);mode="select"; }
            case "relay_unit" -> { ChessGame.require(hit!=null,"请选择运输单位");cargo=hit.id;mode="relay_station";message="请选择接收中继站"; }
            case "relay_station" -> { ChessGame.require(hit!=null&&hit.kind==UnitKind.RELAY,"请选择接收中继站");receiver=hit.id;mode="relay_destination";message="请选择目的地左上角"; }
            case "relay_destination" -> { apply("teleport","unitId",selected,"targetId",cargo,"receiverId",receiver,"x",cell.x(),"y",cell.y());mode="select"; }
            case "sweep" -> {
                ChessGame.require(unit!=null,"请先选择横扫者");ChessGame.Cell anchor=unit.anchor();
                int maxX=unit.cells.stream().mapToInt(ChessGame.Cell::x).max().orElseThrow(),maxY=unit.cells.stream().mapToInt(ChessGame.Cell::y).max().orElseThrow();
                int dx=cell.x()<anchor.x()?-1:cell.x()>maxX?1:0,dy=cell.y()<anchor.y()?-1:cell.y()>maxY?1:0;
                apply("sweep","unitId",unit.id,"dx",dx,"dy",dy);mode="select";
            }
            default -> {
                if(hit!=null&&hit.owner==game.player()) { selected=hit.id;pivot=cell;message=hit.kind.label+" #"+hit.id+" 已选中"; }
                else if(unit!=null&&hit!=null&&hit.owner!=game.player()) apply("attack","unitId",unit.id,"targetId",hit.id);
                else if(unit!=null) {
                    int dx=cell.x()-pivot.x(),dy=cell.y()-pivot.y();ChessGame.require((dx==0) != (dy==0),"请选择同一行／列，或使用方向键移动");
                    int steps=Math.abs(dx)+Math.abs(dy);apply("move","unitId",unit.id,"dx",Integer.signum(dx),"dy",Integer.signum(dy),"steps",steps);pivot=cell;
                }
            }
        }
        uiRevision++;
    }
    private void control(String id) {
        if(id.startsWith("deploy:")) { recipe=UnitKind.valueOf(id.substring(7));mode="deploy";rotation=0;message="按配方在棋盘部署 "+recipe.label+"；R 旋转配方"; }
        else switch(id) {
            case "end_turn" -> apply("end_turn");
            case "standard", "showcase" -> apply("new_game","scenario",id,"commanderA",commanderA,"commanderB",commanderB);
            case "save", "load" -> apply(id,"slot","quick");
            case "select" -> clearSelection();
            case "produce", "repair", "sweep" -> { mode=id;message="请选择"+(id.equals("produce")?"相邻空格":id.equals("repair")?"待维修友军":"横扫方向"); }
            case "teleport" -> { mode="relay_unit";message="请选择待运输的友军"; }
            case "rotate" -> key("R");
            case "expand" -> { mode="expand";message="点击未开放区块扩张；WASD 平移视图"; }
            case "commanderA" -> commanderA=nextCommander(commanderA);
            case "commanderB" -> commanderB=nextCommander(commanderB);
            default -> throw new IllegalArgumentException("Unknown control");
        }
        uiRevision++;
    }
    private static String nextCommander(String value) { List<String> values=List.of("NONE","VANGUARD","ARTIFICER","EXPEDITION");return values.get((values.indexOf(value)+1)%values.size()); }
    public void key(String value) {
        switch(value.toUpperCase(java.util.Locale.ROOT)) {
            case "SPACE", "ENTER" -> apply("end_turn");
            case "ESCAPE" -> { clearSelection();message="已取消操作"; }
            case "W" -> cameraY-=4;case "S" -> cameraY+=4;case "A" -> cameraX-=4;case "D" -> cameraX+=4;
            case "+", "=" -> cellSize=Math.min(52,cellSize+4);case "-" -> cellSize=Math.max(20,cellSize-4);
            case "R" -> {
                if(mode.equals("deploy")) rotation=(rotation+1)%4;
                else { ChessGame.Unit unit=selectedUnit();ChessGame.require(unit!=null,"请先选择可移动的多格单位");apply("rotate","unitId",unit.id,"x",pivot.x(),"y",pivot.y()); }
            }
            case "UP", "DOWN", "LEFT", "RIGHT" -> {
                ChessGame.Unit unit=selectedUnit();ChessGame.require(unit!=null,"请先选择单位");String direction=value.toUpperCase(java.util.Locale.ROOT);
                int dx=direction.equals("LEFT")?-1:direction.equals("RIGHT")?1:0,dy=direction.equals("UP")?-1:direction.equals("DOWN")?1:0;
                apply("move","unitId",unit.id,"dx",dx,"dy",dy);pivot=pivot.move(dx,dy);
            }
            default -> throw new IllegalArgumentException("Unknown UI key: "+value);
        }
        uiRevision++;
    }
    public void userInput(Runnable action) { try { action.run(); } catch(Exception failure) { message=failure.getMessage();uiRevision++; } }
    @Override public byte[] screenshot() throws Exception { return capture.capture(); }
}
