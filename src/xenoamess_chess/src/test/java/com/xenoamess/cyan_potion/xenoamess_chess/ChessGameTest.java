package com.xenoamess.cyan_potion.xenoamess_chess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xenoamess.cyan_potion.mcp.AutomationBridge;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChessGameTest {
    private ChessGame empty() {
        ChessGame game=new ChessGame();game.units.clear();game.actionPoints[0]=30;game.actionPoints[1]=30;return game;
    }
    private JsonNode command(ChessGame game,String name,Object... pairs) {
        ObjectNode args=AutomationBridge.JSON.createObjectNode();
        for(int i=0;i<pairs.length;i+=2) args.set(pairs[i].toString(),AutomationBridge.JSON.valueToTree(pairs[i+1]));
        return game.execute(name,args);
    }
    private void nextOwnTurn(ChessGame game) { command(game,"end_turn");command(game,"end_turn"); }

    @Test void deployedAndProducedUnitsWaitAndFailedCommandIsAtomic() {
        ChessGame game=empty();command(game,"deploy","kind","MINION","x",1,"y",1);
        var minion=game.at(new ChessGame.Cell(1,1));JsonNode before=game.snapshot();
        assertThrows(IllegalArgumentException.class,() -> command(game,"move","unitId",minion.id,"dx",1,"dy",0));assertEquals(before,game.snapshot());
        nextOwnTurn(game);command(game,"move","unitId",minion.id,"dx",1,"dy",0);assertTrue(minion.acted);
        var factory=game.spawn(UnitKind.FACTORY,0,5,5,0);command(game,"produce","unitId",factory.id,"x",7,"y",5);
        assertTrue(game.at(new ChessGame.Cell(7,5)).waiting);
    }
    @Test void damageAccumulatesWithoutDisablingProductionAndBreakupPreservesOwnership() {
        ChessGame game=empty();var attacker=game.spawn(UnitKind.MINION,0,3,6,0);var factory=game.spawn(UnitKind.FACTORY,1,4,6,0);
        command(game,"attack","unitId",attacker.id,"targetId",factory.id);assertEquals(3,factory.hp);
        command(game,"end_turn");command(game,"produce","unitId",factory.id,"x",6,"y",6);command(game,"end_turn");
        for(int i=0;i<3;i++) { command(game,"attack","unitId",attacker.id,"targetId",factory.id);if(i<2) nextOwnTurn(game); }
        assertFalse(game.units.containsKey(factory.id));assertEquals(0,game.score(0));
        for(int x=4;x<=5;x++) for(int y=6;y<=7;y++) {
            var part=game.at(new ChessGame.Cell(x,y));assertEquals(UnitKind.MINION,part.kind);assertEquals(1,part.owner);assertTrue(part.waiting);
        }
    }
    @Test void matchingRecipesConsumeWholeMaterialsAndChargeOnlyOnSuccess() {
        ChessGame game=empty();for(int x=2;x<4;x++) for(int y=2;y<4;y++) game.spawn(UnitKind.MINION,0,x,y,0);
        int before=game.actionPoints(0);command(game,"deploy","kind","FACTORY","x",2,"y",2);
        assertEquals(before-3,game.actionPoints(0));assertEquals(1,game.units().size());assertTrue(game.units().get(0).waiting);
        JsonNode state=game.snapshot();assertThrows(IllegalArgumentException.class,() -> command(game,"deploy","kind","CANNON","x",2,"y",2));assertEquals(state,game.snapshot());
    }
    @Test void allTenRecipesAndFourRotationsSurviveSaveRoundTrip() {
        assertEquals(10,UnitKind.values().length);
        for(UnitKind kind:UnitKind.values()) for(int rotation=0;rotation<4;rotation++) {
            ChessGame game=empty();var unit=game.spawn(kind,0,6,6,rotation);ChessGame restored=new ChessGame();restored.restore(game.snapshot());
            assertEquals(unit.cells,restored.units().get(0).cells);assertEquals(kind,restored.units().get(0).kind);
        }
    }
    @Test void rotatingNestedUnitTransformsEveryLeafBeforeRecursiveBreakup() {
        ChessGame game=empty();var guardian=game.spawn(UnitKind.GUARDIAN,0,4,4,0);
        command(game,"rotate","unitId",guardian.id,"x",4,"y",4,"turns",1);
        var expected=java.util.Set.copyOf(guardian.cells);guardian.hp=1;game.player=1;
        var enemy=game.spawn(UnitKind.MINION,1,5,4,0);command(game,"attack","unitId",enemy.id,"targetId",guardian.id);
        assertEquals(4,game.units().stream().filter(unit -> unit.owner==0&&unit.kind==UnitKind.MINION&&expected.containsAll(unit.cells)).count());
    }
    @Test void sweptLargeTargetIsHitOnceAndNewComponentsAreNotHitAgain() {
        ChessGame game=empty();var sweeper=game.spawn(UnitKind.SWEEPER,0,3,3,0);var factory=game.spawn(UnitKind.FACTORY,1,3,1,0);
        command(game,"sweep","unitId",sweeper.id,"dx",0,"dy",-1);assertEquals(3,factory.hp);
        nextOwnTurn(game);factory.hp=1;command(game,"sweep","unitId",sweeper.id,"dx",0,"dy",-1);
        assertEquals(4,game.units().stream().filter(unit -> unit.owner==1&&unit.kind==UnitKind.MINION).count());assertEquals(0,game.score(0));
    }
    @Test void sweepCleansThreeMinionsInOneAction() {
        ChessGame game=empty();var sweeper=game.spawn(UnitKind.SWEEPER,0,3,3,0);
        for(int x=3;x<=5;x++) game.spawn(UnitKind.MINION,1,x,2,0);
        command(game,"sweep","unitId",sweeper.id,"dx",0,"dy",-1);assertEquals(3,game.score(0));assertEquals(1,game.units().size());
    }
    @Test void rangedAttacksRespectBlockersAndCannonMinimumRange() {
        ChessGame game=empty();var cannon=game.spawn(UnitKind.CANNON,0,1,4,0);var adjacent=game.spawn(UnitKind.MINION,1,4,4,0);
        assertThrows(IllegalArgumentException.class,() -> command(game,"attack","unitId",cannon.id,"targetId",adjacent.id));
        game.units.remove(adjacent.id);var distant=game.spawn(UnitKind.FACTORY,1,6,4,0);var blocker=game.spawn(UnitKind.MINION,0,5,4,0);var lowerBlocker=game.spawn(UnitKind.MINION,0,5,5,0);
        assertThrows(IllegalArgumentException.class,() -> command(game,"attack","unitId",cannon.id,"targetId",distant.id));
        game.units.remove(blocker.id);game.units.remove(lowerBlocker.id);command(game,"attack","unitId",cannon.id,"targetId",distant.id);assertEquals(1,distant.hp);
    }
    @Test void repairPreservesActionStateAndCommanderChangesAmount() {
        ChessGame game=empty();game.commanders[0]="ARTIFICER";var engineer=game.spawn(UnitKind.ENGINEER,0,2,2,0);var target=game.spawn(UnitKind.FACTORY,0,5,2,0);
        target.hp=1;target.acted=true;command(game,"repair","unitId",engineer.id,"targetId",target.id);assertEquals(3,target.hp);assertTrue(target.acted);
    }
    @Test void relayPreservesDamageAndSpentActionAndRequiresReadyStations() {
        ChessGame game=empty();var source=game.spawn(UnitKind.RELAY,0,1,1,0);var receiver=game.spawn(UnitKind.RELAY,0,10,1,0);var target=game.spawn(UnitKind.MINION,0,4,2,0);target.acted=true;
        command(game,"teleport","unitId",source.id,"targetId",target.id,"receiverId",receiver.id,"x",13,"y",2);
        assertEquals(target,game.at(new ChessGame.Cell(13,2)));assertTrue(target.acted);assertTrue(source.acted);assertFalse(receiver.acted);
    }
    @Test void negativeBoardCoordinatesUseFloorDivision() {
        ChessGame game=empty();command(game,"expand","boardX",-1,"boardY",0);command(game,"deploy","kind","MINION","x",-1,"y",0);
        assertNotNull(game.at(new ChessGame.Cell(-1,0)));assertFalse(game.open(new ChessGame.Cell(-17,0)));
    }
    @Test void vanguardIsAnExplicitDeploymentWaitingException() {
        ChessGame game=empty();game.commanders[0]="VANGUARD";game.spawn(UnitKind.MINION,0,1,1,0);game.spawn(UnitKind.MINION,0,2,1,0);
        command(game,"deploy","kind","ASSAULT","x",1,"y",1);var assault=game.at(new ChessGame.Cell(1,1));assertFalse(assault.waiting);
        command(game,"move","unitId",assault.id,"dx",0,"dy",1,"steps",3);assertTrue(assault.acted);
    }
    @Test void invalidSaveIsRejectedWithoutChangingCurrentGame() {
        ChessGame game=new ChessGame();ObjectNode invalid=game.snapshot().deepCopy();((ObjectNode)invalid.path("units").get(0)).put("hp",0);
        JsonNode before=game.snapshot();assertThrows(IllegalArgumentException.class,() -> game.restore(invalid));assertEquals(before,game.snapshot());
    }
    @Test void scoreVictoryAndThirtyRoundDrawFinishTheGame() {
        ChessGame game=empty();game.scores[0]=11;var minion=game.spawn(UnitKind.MINION,0,1,1,0);var enemy=game.spawn(UnitKind.MINION,1,2,1,0);
        command(game,"attack","unitId",minion.id,"targetId",enemy.id);assertEquals(0,game.winner());
        assertThrows(IllegalArgumentException.class,() -> command(game,"end_turn"));
        ChessGame draw=new ChessGame();for(int turn=0;turn<60;turn++) command(draw,"end_turn");assertEquals(2,draw.winner());
    }
}
