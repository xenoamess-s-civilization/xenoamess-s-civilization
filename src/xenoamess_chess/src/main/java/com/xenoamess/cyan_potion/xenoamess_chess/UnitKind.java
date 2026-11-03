package com.xenoamess.cyan_potion.xenoamess_chess;

import java.util.ArrayList;
import java.util.List;

/** The ten public recipes; rotations preserve each ingredient as a whole unit. */
public enum UnitKind {
    MINION("minion", "M", 1, 1, 1, 1, 1, 1, true, "m"),
    FACTORY("工厂", "F", 3, 4, 0, 0, 0, 0, false, "mm/mm"),
    ASSAULT("突击手", "A", 2, 2, 1, 1, 1, 3, true, "mm"),
    LANCER("长枪兵", "L", 2, 3, 2, 1, 2, 1, true, "mmm"),
    ARCHER("射手", "R", 2, 2, 1, 1, 3, 1, false, "mm/m."),
    GUARDIAN("重装卫士", "G", 3, 5, 1, 1, 1, 1, true, "AA/BB"),
    CANNON("攻城炮", "C", 4, 3, 3, 2, 4, 1, false, "AAA/.m."),
    ENGINEER("维修师", "E", 3, 3, 0, 0, 0, 1, false, ".mm/mm."),
    SWEEPER("横扫者", "S", 4, 4, 1, 0, 0, 1, false, "FFR/FFR"),
    RELAY("中继站", "T", 3, 4, 0, 0, 0, 0, false, ".m./mmm/.m.");

    public final String label, symbol, pattern;
    public final int cost, durability, damage, minRange, maxRange, movement;
    public final boolean melee;
    UnitKind(String label, String symbol, int cost, int durability, int damage, int minRange, int maxRange, int movement, boolean melee, String pattern) {
        this.label=label; this.symbol=symbol; this.cost=cost; this.durability=durability; this.damage=damage;
        this.minRange=minRange; this.maxRange=maxRange; this.movement=movement; this.melee=melee; this.pattern=pattern;
    }
    public record Ingredient(UnitKind kind, List<ChessGame.Cell> cells) { }
    public List<Ingredient> recipe(int rotation, int x, int y) {
        if (rotation < 0 || rotation > 3) throw new IllegalArgumentException("rotation must be 0..3");
        String[] rows = pattern.split("/");
        record Mark(char symbol, ChessGame.Cell cell) { }
        List<Mark> marks = new ArrayList<>();
        int minX=Integer.MAX_VALUE, minY=Integer.MAX_VALUE;
        for (int row=0;row<rows.length;row++) for (int column=0;column<rows[row].length();column++) {
            char symbol=rows[row].charAt(column);
            if (symbol=='.') continue;
            ChessGame.Cell cell=new ChessGame.Cell(column,row).rotate(new ChessGame.Cell(0,0),rotation);
            marks.add(new Mark(symbol,cell)); minX=Math.min(minX,cell.x()); minY=Math.min(minY,cell.y());
        }
        List<Ingredient> result=new ArrayList<>();
        java.util.Map<Character,List<ChessGame.Cell>> groups=new java.util.LinkedHashMap<>();
        for (Mark mark:marks) {
            ChessGame.Cell cell=mark.cell.move(x-minX,y-minY);
            if (mark.symbol=='m') result.add(new Ingredient(MINION,List.of(cell)));
            else groups.computeIfAbsent(mark.symbol, key -> new ArrayList<>()).add(cell);
        }
        for (var group:groups.entrySet()) {
            UnitKind kind=switch (this) { case GUARDIAN -> ASSAULT; case CANNON -> LANCER;
                case SWEEPER -> group.getKey()=='F' ? FACTORY : ASSAULT;
                default -> throw new IllegalStateException("Invalid recipe"); };
            result.add(new Ingredient(kind,List.copyOf(group.getValue())));
        }
        return result;
    }
    public List<ChessGame.Cell> cells(int rotation,int x,int y) { return recipe(rotation,x,y).stream().flatMap(part -> part.cells.stream()).toList(); }
}
