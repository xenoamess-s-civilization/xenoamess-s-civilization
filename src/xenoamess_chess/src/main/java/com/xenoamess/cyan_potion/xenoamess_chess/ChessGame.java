package com.xenoamess.cyan_potion.xenoamess_chess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xenoamess.cyan_potion.mcp.AutomationBridge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/** Deterministic rules shared by native UI, MCP and saved games. No GL dependency. */
public final class ChessGame {
    public record Cell(int x,int y) {
        public Cell move(int dx,int dy) { return new Cell(Math.addExact(x,dx),Math.addExact(y,dy)); }
        public Cell rotate(Cell pivot,int turns) {
            int dx=x-pivot.x,dy=y-pivot.y;
            for (int i=0;i<turns;i++) { int next=-dy; dy=dx; dx=next; }
            return new Cell(Math.addExact(pivot.x,dx),Math.addExact(pivot.y,dy));
        }
    }
    public static final class Unit {
        public long id;
        public UnitKind kind;
        public int owner,hp;
        public boolean waiting,acted;
        public List<Cell> cells=new ArrayList<>();
        public List<Unit> components=new ArrayList<>();
        public Unit() { }
        Unit(long id,UnitKind kind,int owner,List<Cell> cells,List<Unit> components) {
            this.id=id;this.kind=kind;this.owner=owner;this.hp=kind.durability;
            this.cells=new ArrayList<>(cells);this.components=new ArrayList<>(components);
        }
        void transform(UnaryOperator<Cell> operation) { cells=new ArrayList<>(cells.stream().map(operation).toList()); components.forEach(part -> part.transform(operation)); }
        public boolean canAct() { return !waiting&&!acted; }
        public Cell anchor() { return new Cell(cells.stream().mapToInt(Cell::x).min().orElseThrow(),cells.stream().mapToInt(Cell::y).min().orElseThrow()); }
    }
    final Map<Long,Unit> units=new LinkedHashMap<>();
    final Set<Cell> boards=new LinkedHashSet<>();
    final int[] actionPoints={0,0},scores={0,0};
    final String[] commanders={"NONE","NONE"};
    int player,ply=1,winner=-1;
    long nextId=1,revision;
    String scenario="standard",lastAction="开始对局";

    public ChessGame() { reset("standard","NONE","NONE"); }
    public List<Unit> units() { return List.copyOf(units.values()); }
    public int player() { return player; }
    public int round() { return (ply+1)/2; }
    public int winner() { return winner; }
    public int actionPoints(int owner) { return actionPoints[owner]; }
    public int score(int owner) { return scores[owner]; }
    public String commander(int owner) { return commanders[owner]; }
    public String lastAction() { return lastAction; }
    public Set<Cell> boards() { return Set.copyOf(boards); }
    public Unit at(Cell cell) { return units.values().stream().filter(unit -> unit.cells.contains(cell)).findFirst().orElse(null); }
    public boolean open(Cell cell) { return boards.contains(new Cell(Math.floorDiv(cell.x,16),Math.floorDiv(cell.y,16))); }
    public static void require(boolean condition,String message) { if(!condition) throw new IllegalArgumentException(message); }
    public static int integer(JsonNode arguments,String key,int fallback) {
        JsonNode value=arguments.get(key); if(value==null) return fallback;
        require(value.isIntegralNumber()&&value.canConvertToInt(),key+" must be an integer"); return value.intValue();
    }
    public static long identifier(JsonNode args,String key) { JsonNode value=args.get(key); require(value!=null&&value.isIntegralNumber()&&value.canConvertToLong(),key+" must be an integer unit ID"); return value.longValue(); }
    private Unit unit(long id) { Unit unit=units.get(id);require(unit!=null,"单位不存在："+id);return unit; }
    private Unit actor(long id) { Unit unit=unit(id);require(unit.owner==player,"只能操作当前玩家的单位");require(unit.canAct(),unit.waiting?"新入场单位要等待下个己方回合":"该单位本回合已经行动");return unit; }
    private void pay(int cost) { require(actionPoints[player]>=cost,"行动力不足，需要 "+cost); }

    public JsonNode execute(String name,JsonNode args) {
        require(args.isObject(),"arguments must be an object");
        if(name.equals("new_game")) {
            reset(args.path("scenario").asText("standard"),args.path("commanderA").asText("NONE"),args.path("commanderB").asText("NONE"));return snapshot();
        }
        require(winner==-1,"对局已结束，请开始新对局");
        switch(name) {
            case "end_turn" -> endTurn();
            case "deploy" -> deploy(UnitKind.valueOf(args.path("kind").asText().toUpperCase(java.util.Locale.ROOT)),integer(args,"x",0),integer(args,"y",0),integer(args,"rotation",0));
            case "move" -> move(identifier(args,"unitId"),integer(args,"dx",0),integer(args,"dy",0),integer(args,"steps",1),args.has("targetId")?identifier(args,"targetId"):-1);
            case "attack" -> attack(identifier(args,"unitId"),identifier(args,"targetId"));
            case "rotate" -> rotate(identifier(args,"unitId"),integer(args,"x",0),integer(args,"y",0),integer(args,"turns",1));
            case "produce" -> produce(identifier(args,"unitId"),new Cell(integer(args,"x",0),integer(args,"y",0)));
            case "repair" -> repair(identifier(args,"unitId"),identifier(args,"targetId"));
            case "sweep" -> sweep(identifier(args,"unitId"),integer(args,"dx",0),integer(args,"dy",0));
            case "teleport" -> teleport(identifier(args,"unitId"),identifier(args,"targetId"),identifier(args,"receiverId"),integer(args,"x",0),integer(args,"y",0));
            case "expand" -> expand(integer(args,"boardX",0),integer(args,"boardY",0));
            case "resign" -> { winner=1-player;lastAction=(player==0?"A":"B")+" 方认输"; }
            default -> throw new IllegalArgumentException("Unknown game command: "+name);
        }
        revision++;
        if(winner==-1&&scores[player]>=12) winner=player;
        return snapshot();
    }

    public void reset(String requested,String a,String b) {
        require(Set.of("standard","showcase").contains(requested),"scenario must be standard or showcase");
        Set<String> allowed=Set.of("NONE","VANGUARD","ARTIFICER","EXPEDITION");
        require(allowed.contains(a)&&allowed.contains(b),"Unknown commander");
        units.clear();boards.clear();boards.add(new Cell(0,0));Arrays.fill(scores,0);Arrays.fill(actionPoints,0);
        commanders[0]=a;commanders[1]=b;player=0;ply=1;winner=-1;nextId=1;scenario=requested;
        if(requested.equals("standard")) {
            spawn(UnitKind.FACTORY,0,2,6,0);spawn(UnitKind.MINION,0,4,6,0);spawn(UnitKind.MINION,0,4,7,0);
            spawn(UnitKind.FACTORY,1,12,6,0);spawn(UnitKind.MINION,1,11,6,0);spawn(UnitKind.MINION,1,11,7,0);
            actionPoints[0]=1;
        } else {
            int[][] positions={{1,1},{1,4},{4,3},{1,8},{5,4},{4,8},{1,11},{5,11},{1,14},{5,0}};
            for(int owner=0;owner<2;owner++) for(UnitKind kind:UnitKind.values()) {
                int[] position=positions[kind.ordinal()];spawn(kind,owner,position[0]+owner*8,position[1],0);
            }
            actionPoints[0]=12;actionPoints[1]=12;
        }
        revision++;lastAction=requested.equals("standard")?"标准对局：A 方开始":"展示局：十种单位已就绪，双方各有 12 行动力";
    }
    private Unit build(UnitKind kind,int owner,int x,int y,int rotation) {
        List<Unit> parts=new ArrayList<>();
        if(kind!=UnitKind.MINION) for(var ingredient:kind.recipe(rotation,x,y)) {
            Cell anchor=anchor(ingredient.cells());
            int sourceRotation=0;
            while(sourceRotation<4&&!new HashSet<>(ingredient.kind().cells(sourceRotation,anchor.x,anchor.y)).equals(new HashSet<>(ingredient.cells()))) sourceRotation++;
            require(sourceRotation<4,"Invalid component orientation");
            parts.add(build(ingredient.kind(),owner,anchor.x,anchor.y,sourceRotation));
        }
        return new Unit(nextId++,kind,owner,kind.cells(rotation,x,y),parts);
    }
    Unit spawn(UnitKind kind,int owner,int x,int y,int rotation) {
        Unit result=build(kind,owner,x,y,rotation);units.put(result.id,result);return result;
    }
    private static Cell anchor(List<Cell> cells) { return new Cell(cells.stream().mapToInt(Cell::x).min().orElseThrow(),cells.stream().mapToInt(Cell::y).min().orElseThrow()); }
    private static boolean adjacent(List<Cell> first,List<Cell> second) { return distance(first,second)==1; }
    private static int distance(List<Cell> first,List<Cell> second) { return first.stream().flatMap(a -> second.stream().map(b -> Math.abs(a.x-b.x)+Math.abs(a.y-b.y))).mapToInt(Integer::intValue).min().orElseThrow(); }
    private void empty(List<Cell> cells,Unit ignore) {
        for(Cell cell:cells) { require(open(cell),"目标位于未开放棋盘");Unit occupying=at(cell);require(occupying==null||occupying==ignore,"目标位置已被占用"); }
    }
    private void deploy(UnitKind kind,int x,int y,int rotation) {
        pay(kind.cost);List<Cell> cells=kind.cells(rotation,x,y);
        for(Cell cell:cells) require(open(cell),"目标位于未开放棋盘");
        List<Unit> parts=new ArrayList<>();
        if(kind==UnitKind.MINION) empty(cells,null);
        else for(var ingredient:kind.recipe(rotation,x,y)) {
            Unit part=at(ingredient.cells().get(0));
            require(part!=null&&part.owner==player&&part.kind==ingredient.kind(),"祭品类型或归属不符合配方");
            require(part.hp==part.kind.durability,"祭品必须满耐久");
            require(new HashSet<>(part.cells).equals(new HashSet<>(ingredient.cells())),"祭品形状或朝向不符合配方");
            require(!parts.contains(part),"不能重复使用同一祭品");parts.add(part);
        }
        Unit result=new Unit(nextId++,kind,player,cells,parts);
        result.waiting=!(kind==UnitKind.ASSAULT&&commanders[player].equals("VANGUARD"));
        parts.forEach(part -> units.remove(part.id));units.put(result.id,result);actionPoints[player]-=kind.cost;
        lastAction="部署 "+kind.label;
    }
    private void endTurn() {
        if(ply>=60) { winner=scores[0]==scores[1]?2:scores[0]>scores[1]?0:1;lastAction="30 轮结束，按清除分结算";return; }
        player=1-player;ply++;actionPoints[player]++;
        for(Unit unit:units.values()) if(unit.owner==player) { unit.waiting=false;unit.acted=false; }
        lastAction=(player==0?"A":"B")+" 方回合";
    }
    private void move(long id,int dx,int dy,int steps,long requestedTarget) {
        Unit actor=actor(id);require(actor.kind.movement>0,"固定建筑不能移动");
        require(Math.abs(dx)+Math.abs(dy)==1,"移动方向必须是上下左右");
        require(steps>=1&&steps<=actor.kind.movement,"移动步数超出能力范围");
        List<Cell> planned=actor.cells;int moved=0;Unit target=null;
        for(int i=0;i<steps;i++) {
            List<Cell> next=planned.stream().map(cell -> cell.move(dx,dy)).toList();
            boolean blocked=false;Set<Unit> enemies=new LinkedHashSet<>();
            for(Cell cell:next) {
                Unit occupying=at(cell);
                if(!open(cell)||(occupying!=null&&occupying!=actor&&occupying.owner==player)) { blocked=true;break; }
                if(occupying!=null&&occupying!=actor) enemies.add(occupying);
            }
            if(blocked) break;
            if(!enemies.isEmpty()) {
                if(actor.kind.melee) {
                    target=requestedTarget<0?enemies.iterator().next():enemies.stream().filter(enemy -> enemy.id==requestedTarget).findFirst().orElse(null);
                    require(target!=null,"指定目标不在接触位置");
                }
                break;
            }
            planned=next;moved++;
        }
        require(moved>0||target!=null,"前方被阻挡，无法移动或攻击");
        int total=moved;actor.transform(cell -> cell.move(dx*total,dy*total));actor.acted=true;
        if(target!=null) damage(target,actor.kind.damage);
        lastAction=actor.kind.label+(target==null?"移动 "+moved+" 格":"接敌攻击");
    }
    private boolean ray(Unit attacker,Unit target,int minimum,int maximum) {
        for(Cell origin:attacker.cells) for(int[] dir:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            for(int range=1;range<=maximum;range++) {
                Cell cell=origin.move(dir[0]*range,dir[1]*range);if(!open(cell)) break;
                Unit hit=at(cell);
                if(hit!=null) { if(hit==target&&range>=minimum) return true;break; }
            }
        }
        return false;
    }
    private void attack(long id,long targetId) {
        Unit actor=actor(id),target=unit(targetId);
        require(target.owner!=player,"不能攻击友军");require(actor.kind.damage>0&&actor.kind!=UnitKind.SWEEPER,"该单位没有普通攻击");
        boolean legal=(actor.kind.melee&&adjacent(actor.cells,target.cells))
                ||((actor.kind==UnitKind.LANCER||actor.kind==UnitKind.ARCHER||actor.kind==UnitKind.CANNON)&&ray(actor,target,actor.kind.minRange,actor.kind.maxRange));
        require(legal,"目标不在合法攻击范围或射线被阻挡");actor.acted=true;damage(target,actor.kind.damage);lastAction=actor.kind.label+"攻击 "+target.kind.label;
    }
    private void damage(Unit target,int amount) {
        target.hp=Math.max(0,target.hp-amount);if(target.hp>0) return;
        units.remove(target.id);
        if(target.components.isEmpty()) scores[player]++;
        else release(target);
    }
    private void release(Unit target) {
        for(Unit part:target.components) {
            if(!part.components.isEmpty()) release(part);
            else { part.hp=part.kind.durability;part.waiting=true;part.acted=false;units.put(part.id,part); }
        }
    }
    private void rotate(long id,int x,int y,int turns) {
        Unit actor=actor(id);require(actor.kind.movement>0&&actor.cells.size()>1,"该单位不能转向");
        Cell pivot=new Cell(x,y);require(actor.cells.contains(pivot),"转向支点必须是单位当前占格");require(turns>=1&&turns<=3,"turns must be 1..3");
        List<Cell> rotated=actor.cells.stream().map(cell -> cell.rotate(pivot,turns)).toList();empty(rotated,actor);
        actor.transform(cell -> cell.rotate(pivot,turns));actor.acted=true;lastAction=actor.kind.label+"转向";
    }
    private void produce(long id,Cell destination) {
        Unit factory=actor(id);require(factory.kind==UnitKind.FACTORY,"只有工厂能生产");
        require(adjacent(factory.cells,List.of(destination)),"生产位置须正交贴邻工厂");empty(List.of(destination),null);
        Unit minion=spawn(UnitKind.MINION,player,destination.x,destination.y,0);minion.waiting=true;factory.acted=true;lastAction="工厂生产 minion";
    }
    private void repair(long id,long targetId) {
        Unit engineer=actor(id),target=unit(targetId);require(engineer.kind==UnitKind.ENGINEER,"只有维修师能维修");
        require(target!=engineer&&target.owner==player&&adjacent(engineer.cells,target.cells),"维修需要相邻的其他友军");require(target.hp<target.kind.durability,"目标耐久已满");
        target.hp=Math.min(target.kind.durability,target.hp+(commanders[player].equals("ARTIFICER")?2:1));engineer.acted=true;lastAction="维修 "+target.kind.label;
    }
    private void sweep(long id,int dx,int dy) {
        Unit actor=actor(id);require(actor.kind==UnitKind.SWEEPER,"只有横扫者能横扫");require(Math.abs(dx)+Math.abs(dy)==1,"请选择横扫方向");
        Set<Cell> cells=new HashSet<>(actor.cells);List<Cell> edge=actor.cells.stream().map(cell -> cell.move(dx,dy)).filter(cell -> !cells.contains(cell)).toList();
        require(edge.size()==3,"必须选择三格长边的外侧");
        Set<Unit> targets=new LinkedHashSet<>();for(Cell cell:edge) { Unit hit=at(cell);if(hit!=null&&hit.owner!=player) targets.add(hit); }
        require(!targets.isEmpty(),"横扫范围内没有敌军");actor.acted=true;targets.forEach(target -> damage(target,actor.kind.damage));lastAction="横扫 "+targets.size()+" 个目标";
    }
    private void teleport(long id,long targetId,long receiverId,int x,int y) {
        Unit source=actor(id),target=unit(targetId),receiver=unit(receiverId);
        require(source.kind==UnitKind.RELAY&&receiver.kind==UnitKind.RELAY&&receiver!=source&&receiver.owner==player&&!receiver.waiting,"需要另一座已就绪的己方中继站");
        require(target.owner==player&&target.kind!=UnitKind.RELAY&&!target.waiting,"只能传送已就绪的己方非中继站单位");
        require(distance(source.cells,target.cells)<=(commanders[player].equals("EXPEDITION")?2:1),"目标不在来源站的中继范围");
        Cell origin=target.anchor();int dx=x-origin.x,dy=y-origin.y;
        List<Cell> destination=target.cells.stream().map(cell -> cell.move(dx,dy)).toList();empty(destination,target);require(adjacent(destination,receiver.cells),"落点须贴邻接收站");
        target.transform(cell -> cell.move(dx,dy));source.acted=true;lastAction="中继传送 "+target.kind.label;
    }
    private void expand(int x,int y) {
        require(Math.abs((long)x)<=1_000_000&&Math.abs((long)y)<=1_000_000,"区块坐标超出实现范围");Cell board=new Cell(x,y);
        require(!boards.contains(board),"棋盘已开放");require(boards.stream().anyMatch(other -> Math.abs((long)other.x-x)+Math.abs((long)other.y-y)==1),"新棋盘必须与现有棋盘共享一条边");
        pay(3);boards.add(board);actionPoints[player]-=3;lastAction="扩张棋盘 ("+x+","+y+")";
    }
    public ObjectNode snapshot() {
        ObjectNode root=AutomationBridge.JSON.createObjectNode().put("schemaVersion",1).put("revision",revision).put("scenario",scenario)
                .put("player",player).put("ply",ply).put("round",round()).put("winner",winner).put("nextId",nextId).put("lastAction",lastAction);
        root.set("actionPoints",AutomationBridge.JSON.valueToTree(actionPoints));root.set("scores",AutomationBridge.JSON.valueToTree(scores));
        root.set("commanders",AutomationBridge.JSON.valueToTree(commanders));root.set("boards",AutomationBridge.JSON.valueToTree(boards));root.set("units",AutomationBridge.JSON.valueToTree(units.values()));return root;
    }
    public void restore(JsonNode data) {
        try {
            require(data.path("schemaVersion").asInt()==1,"不支持的存档版本");
            int restoredPlayer=data.path("player").asInt(-1),restoredPly=data.path("ply").asInt(0),restoredWinner=data.path("winner").asInt(-2);
            require(restoredPlayer>=0&&restoredPlayer<=1&&restoredPly>=1&&restoredPly<=60&&restoredPlayer==(restoredPly-1)%2,"存档回合无效");
            require(restoredWinner>=-1&&restoredWinner<=2,"存档胜负状态无效");
            Set<Cell> restoredBoards=new LinkedHashSet<>(Arrays.asList(AutomationBridge.JSON.treeToValue(data.path("boards"),Cell[].class)));
            require(!restoredBoards.isEmpty(),"存档没有棋盘");
            Map<Long,Unit> restoredUnits=new LinkedHashMap<>();Set<Cell> occupied=new HashSet<>();Set<Long> identifiers=new HashSet<>();
            for(Unit unit:AutomationBridge.JSON.treeToValue(data.path("units"),Unit[].class)) {
                validateUnit(unit,identifiers);
                for(Cell cell:unit.cells) require(restoredBoards.contains(new Cell(Math.floorDiv(cell.x,16),Math.floorDiv(cell.y,16)))&&occupied.add(cell),"存档单位越界或重叠");
                restoredUnits.put(unit.id,unit);
            }
            long restoredNext=data.path("nextId").asLong();require(identifiers.stream().allMatch(id -> id<restoredNext),"存档单位序号无效");
            int[] ap=AutomationBridge.JSON.treeToValue(data.path("actionPoints"),int[].class),points=AutomationBridge.JSON.treeToValue(data.path("scores"),int[].class);
            String[] leaders=AutomationBridge.JSON.treeToValue(data.path("commanders"),String[].class);
            require(ap.length==2&&points.length==2&&leaders.length==2&&Arrays.stream(ap).allMatch(value -> value>=0)&&Arrays.stream(points).allMatch(value -> value>=0),"存档资源无效");
            require(Arrays.stream(leaders).allMatch(value -> Set.of("NONE","VANGUARD","ARTIFICER","EXPEDITION").contains(value)),"存档指挥官无效");
            units.clear();units.putAll(restoredUnits);boards.clear();boards.addAll(restoredBoards);player=restoredPlayer;ply=restoredPly;winner=restoredWinner;nextId=restoredNext;
            System.arraycopy(ap,0,actionPoints,0,2);System.arraycopy(points,0,scores,0,2);System.arraycopy(leaders,0,commanders,0,2);
            scenario=data.path("scenario").asText("standard");revision=data.path("revision").asLong()+1;lastAction="已读取存档";
        } catch(IllegalArgumentException failure) { throw failure; }
        catch(Exception failure) { throw new IllegalArgumentException("存档格式无效",failure); }
    }
    private static void validateUnit(Unit unit,Set<Long> ids) {
        require(unit!=null&&unit.kind!=null&&unit.id>0&&ids.add(unit.id)&&unit.owner>=0&&unit.owner<=1,"存档单位身份无效");
        require(unit.hp>=1&&unit.hp<=unit.kind.durability,"存档耐久无效");
        Set<Cell> cells=new HashSet<>(unit.cells);require(cells.size()==unit.cells.size(),"存档占格重复");Cell origin=anchor(unit.cells);
        require(java.util.stream.IntStream.range(0,4).anyMatch(rotation -> new HashSet<>(unit.kind.cells(rotation,origin.x,origin.y)).equals(cells)),"存档单位形状无效");
        if(unit.kind==UnitKind.MINION) require(unit.components.isEmpty(),"minion 不能包含组件");
        else {
            Set<Cell> partCells=new HashSet<>();
            for(Unit part:unit.components) { validateUnit(part,ids);require(part.owner==unit.owner,"组件归属无效");for(Cell cell:part.cells) require(partCells.add(cell),"组件重叠"); }
            require(partCells.equals(cells),"组件不能覆盖单位形状");
            require(java.util.stream.IntStream.range(0,4).anyMatch(rotation -> {
                List<UnitKind.Ingredient> recipe=unit.kind.recipe(rotation,origin.x,origin.y);
                return recipe.size()==unit.components.size()&&recipe.stream().allMatch(ingredient -> unit.components.stream().anyMatch(part -> part.kind==ingredient.kind()&&new HashSet<>(part.cells).equals(new HashSet<>(ingredient.cells()))));
            }),"组件不符合单位配方");
        }
    }
}
