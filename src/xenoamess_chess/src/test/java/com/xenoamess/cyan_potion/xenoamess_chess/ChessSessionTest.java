package com.xenoamess.cyan_potion.xenoamess_chess;

import com.fasterxml.jackson.databind.JsonNode;
import com.xenoamess.cyan_potion.mcp.AutomationBridge;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChessSessionTest {
    @Test void pixelClicksAndKeyboardUseTheSameRulesAsGameCommands() throws Exception {
        ChessSession session=new ChessSession(Files.createTempDirectory("chess-ui"));
        session.apply("ui_click","controlId","deploy:MINION");
        session.apply("ui_click","x",ChessCanvas.BOARD_X+5*36+18,"y",ChessCanvas.BOARD_Y+5*36+18);
        var minion=session.game.at(new ChessGame.Cell(5,5));assertNotNull(minion);assertTrue(minion.waiting);
        session.apply("ui_key","key","SPACE");session.apply("ui_key","key","SPACE");
        session.apply("ui_click","x",ChessCanvas.BOARD_X+5*36+18,"y",ChessCanvas.BOARD_Y+5*36+18);
        session.apply("ui_key","key","RIGHT");assertEquals(minion,session.game.at(new ChessGame.Cell(6,5)));
        JsonNode before=session.game.snapshot();assertThrows(IllegalArgumentException.class,() -> session.apply("ui_key","key","RIGHT"));assertEquals(before,session.game.snapshot());
    }
    @Test void factoryCanBeSelectedAndOperatedEntirelyThroughVisibleControls() throws Exception {
        ChessSession session=new ChessSession(Files.createTempDirectory("chess-factory-ui"));
        session.apply("ui_click","x",ChessCanvas.BOARD_X+2*36+18,"y",ChessCanvas.BOARD_Y+6*36+18);
        session.apply("ui_click","controlId","produce");
        session.apply("ui_click","x",ChessCanvas.BOARD_X+2*36+18,"y",ChessCanvas.BOARD_Y+5*36+18);
        assertTrue(session.game.at(new ChessGame.Cell(2,5)).waiting);
    }
    @Test void saveSlotsRoundTripAndRejectTraversal() throws Exception {
        var directory=Files.createTempDirectory("chess-saves");ChessSession session=new ChessSession(directory);
        session.apply("save","slot","match");session.apply("end_turn");session.apply("load","slot","match");assertEquals(0,session.game.player());
        assertThrows(IllegalArgumentException.class,() -> session.apply("save","slot","../escape"));
        Files.delete(directory.resolve("match.json"));Files.delete(directory);
    }
    @Test void commandSchemasRejectMissingUnknownAndWrongTypedArguments() throws Exception {
        ChessSession session=new ChessSession(Files.createTempDirectory("chess-schema"));JsonNode before=session.game.snapshot();
        assertThrows(IllegalArgumentException.class,() -> session.apply("deploy","kind","MINION"));
        assertThrows(IllegalArgumentException.class,() -> session.apply("end_turn","cheat",true));
        assertThrows(IllegalArgumentException.class,() -> session.apply("deploy","kind","MINION","x","5","y",5));assertEquals(before,session.game.snapshot());
    }
    @Test void screenshotIsARealPngAndStateExplicitlyIdentifiesHeadlessMode() throws Exception {
        ChessSession session=new ChessSession(Files.createTempDirectory("chess-image"));
        session.apply("new_game","scenario","showcase");assertEquals(20,session.game.units().size());
        var image=ImageIO.read(new ByteArrayInputStream(session.screenshot()));assertEquals(1280,image.getWidth());assertEquals(820,image.getHeight());
        assertEquals("headless",session.state().path("runtime").asText());assertEquals(10,java.util.Arrays.stream(UnitKind.values()).count());
        assertTrue(session.commands().stream().allMatch(command -> command.inputSchema().path("type").asText().equals("object")));
    }
}
