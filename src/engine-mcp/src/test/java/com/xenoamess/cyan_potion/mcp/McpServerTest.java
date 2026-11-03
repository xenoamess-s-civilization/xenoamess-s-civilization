package com.xenoamess.cyan_potion.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McpServerTest {
    private static String rpc(int id, String method, Object parameters) throws Exception {
        return AutomationBridge.JSON.writeValueAsString(Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", parameters));
    }
    private static void initialize(McpServer server) throws Exception {
        assertEquals("2025-11-25", server.process(rpc(1, "initialize", Map.of("protocolVersion", "2025-11-25"))).path("result").path("protocolVersion").asText());
        assertNull(server.process("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));
    }
    @Test void protocolLifecycleAndErrors() throws Exception {
        McpServer server = new McpServer(Path.of("missing-session"));
        assertEquals(-32700, server.process("{").path("error").path("code").asInt());
        assertEquals(-32600, server.process("[]").path("error").path("code").asInt());
        assertEquals(-32002, server.process(rpc(1, "tools/list", Map.of())).path("error").path("code").asInt());
        initialize(server);
        assertEquals(5, server.process(rpc(2, "tools/list", Map.of())).path("result").path("tools").size());
        assertEquals(-32602, server.process(rpc(3, "tools/call", Map.of("name", "missing"))).path("error").path("code").asInt());
        assertTrue(server.process(rpc(4, "tools/call", Map.of("name", "engine_status"))).path("result").path("isError").asBoolean());
        assertNull(server.process("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\"}"));
    }
    @Test void commandsTravelThroughLoopbackAndOwnerQueue() throws Exception {
        Path directory = Files.createTempDirectory("engine-mcp-test");
        Path session = directory.resolve("session.json");
        AutomationTarget target = new AutomationTarget() {
            private int value;
            public JsonNode state() { return AutomationBridge.JSON.valueToTree(Map.of("value", value)); }
            public List<Command> commands() { return List.of(new Command("increment", "Increment counter", AutomationBridge.JSON.createObjectNode().put("type", "object"))); }
            public JsonNode execute(String name, JsonNode arguments) { value++; return state(); }
            public byte[] screenshot() { return new byte[]{1, 2, 3}; }
        };
        var owner = Executors.newSingleThreadScheduledExecutor();
        try (AutomationBridge bridge = new AutomationBridge(target, session, 0)) {
            owner.scheduleAtFixedRate(bridge::drain, 0, 5, TimeUnit.MILLISECONDS);
            McpServer server = new McpServer(session);
            initialize(server);
            JsonNode result = server.process(rpc(2, "tools/call", Map.of("name", "engine_command", "arguments", Map.of("name", "increment", "arguments", Map.of()))));
            assertFalse(result.path("result").path("isError").asBoolean());
            assertEquals(1, result.path("result").path("structuredContent").path("data").path("value").asInt());
            assertEquals(1, server.process(rpc(3, "tools/call", Map.of("name", "engine_events"))).path("result").path("structuredContent").path("data").size());
            assertEquals("image", server.process(rpc(4, "tools/call", Map.of("name", "engine_screenshot"))).path("result").path("content").get(0).path("type").asText());
            JsonNode credentials = AutomationBridge.JSON.readTree(session.toFile());
            var response = java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(credentials.path("url").asText()))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}" )).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(401, response.statusCode());
        } finally { owner.shutdownNow(); Files.deleteIfExists(session); Files.deleteIfExists(directory); }
    }
}
