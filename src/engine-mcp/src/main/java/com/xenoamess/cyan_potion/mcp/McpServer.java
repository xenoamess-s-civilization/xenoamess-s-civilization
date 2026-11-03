package com.xenoamess.cyan_potion.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** MCP stdio transport pinned to the 2025-11-25 handshake protocol. */
public final class McpServer {
    private final Path sessionFile;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private boolean initialized;
    private boolean ready;

    public McpServer(Path sessionFile) { this.sessionFile = sessionFile; }

    public static void main(String[] args) throws Exception {
        Path session = Path.of(args.length == 0 ? "src/xenoamess_chess/target/automation-session.json" : args[0]);
        new McpServer(session).run(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                new PrintWriter(System.out, true, StandardCharsets.UTF_8));
    }

    public void run(BufferedReader input, PrintWriter output) throws Exception {
        String line;
        while ((line = input.readLine()) != null) {
            if (line.isBlank()) continue;
            JsonNode response = process(line);
            if (response != null) { output.println(AutomationBridge.JSON.writeValueAsString(response)); output.flush(); }
        }
    }

    public JsonNode process(String line) {
        JsonNode request;
        try { request = AutomationBridge.JSON.readTree(line); }
        catch (Exception invalid) { return error(null, -32700, "Parse error"); }
        if (request == null || !request.isObject() || !"2.0".equals(request.path("jsonrpc").asText())
                || !request.path("method").isTextual() || (request.has("id") && !request.get("id").isTextual() && !request.get("id").isIntegralNumber())) {
            return error(null, -32600, "Invalid request");
        }
        String method = request.path("method").asText();
        if (!request.has("id")) {
            if ("notifications/initialized".equals(method) && initialized) ready = true;
            return null;
        }
        JsonNode id = request.get("id");
        try {
            if ("initialize".equals(method)) {
                if (initialized) return error(id, -32600, "Already initialized");
                initialized = true;
                return result(id, Map.of("protocolVersion", "2025-11-25", "capabilities", Map.of("tools", Map.of()),
                        "serverInfo", Map.of("name", "cyan-potion-engine", "version", "0.1.0"),
                        "instructions", "Inspect engine_commands and engine_status before changing state. Use game commands or ui_click/ui_key; both share the game rules. Take engine_screenshot to verify the displayed result. Failed commands must leave game state unchanged. The engine must be running; session discovery is local to this checkout."));
            }
            if ("ping".equals(method)) return result(id, Map.of());
            if (!ready) return error(id, -32002, "Initialize and send notifications/initialized first");
            return switch (method) {
                case "tools/list" -> result(id, Map.of("tools", tools()));
                case "tools/call" -> call(id, request.path("params"));
                default -> error(id, -32601, "Method not found: " + method);
            };
        } catch (Exception failure) { return error(id, -32603, failure.getMessage() == null ? failure.toString() : failure.getMessage()); }
    }

    public static List<Map<String, Object>> tools() {
        return List.of(tool("engine_status", "Read the running engine and game state", Map.of(), List.of(), true),
                tool("engine_commands", "Discover game commands and their JSON schemas", Map.of(), List.of(), true),
                tool("engine_command", "Execute a discovered command on the game thread", Map.of("name", Map.of("type", "string"), "arguments", Map.of("type", "object")), List.of("name", "arguments"), false),
                tool("engine_screenshot", "Capture the current rendered game frame as PNG", Map.of(), List.of(), true),
                tool("engine_events", "Read recent executed commands after a sequence number", Map.of("after", Map.of("type", "integer", "minimum", 0)), List.of(), true));
    }

    private static Map<String, Object> tool(String name, String description, Map<String, ?> properties, List<String> required, boolean readOnly) {
        return Map.of("name", name, "description", description, "inputSchema", Map.of("type", "object", "properties", properties, "required", required, "additionalProperties", false),
                "annotations", Map.of("readOnlyHint", readOnly, "destructiveHint", !readOnly, "openWorldHint", false));
    }

    private JsonNode call(JsonNode id, JsonNode parameters) {
        String tool = parameters.path("name").asText();
        if (tools().stream().noneMatch(definition -> definition.get("name").equals(tool))) return error(id, -32602, "Unknown tool: " + tool);
        JsonNode arguments = parameters.path("arguments");
        if (arguments.isMissingNode()) arguments = AutomationBridge.JSON.createObjectNode();
        if (!arguments.isObject()) return error(id, -32602, "arguments must be an object");
        ObjectNode operation = AutomationBridge.JSON.createObjectNode();
        try {
            switch (tool) {
                case "engine_status" -> operation.put("operation", "status");
                case "engine_commands" -> operation.put("operation", "commands");
                case "engine_screenshot" -> operation.put("operation", "screenshot");
                case "engine_events" -> {
                    if (arguments.has("after") && (!arguments.get("after").canConvertToLong() || arguments.get("after").asLong() < 0)) throw new IllegalArgumentException("after must be a nonnegative integer");
                    operation.put("operation", "events").put("after", arguments.path("after").asLong(0));
                }
                case "engine_command" -> {
                    if (!arguments.path("name").isTextual() || !arguments.path("arguments").isObject()) throw new IllegalArgumentException("name and object arguments are required");
                    operation.put("operation", "command").put("name", arguments.path("name").asText()).set("arguments", arguments.get("arguments"));
                }
                default -> throw new IllegalArgumentException("Unknown tool");
            }
            JsonNode response = request(operation);
            if ("engine_screenshot".equals(tool)) return result(id, Map.of("content", List.of(Map.of("type", "image", "mimeType", "image/png", "data", response.path("data").asText()))));
            return result(id, Map.of("content", List.of(Map.of("type", "text", "text", AutomationBridge.JSON.writeValueAsString(response))),
                    "structuredContent", Map.of("data", response), "isError", false));
        } catch (Exception failure) {
            return result(id, Map.of("content", List.of(Map.of("type", "text", "text", failure.getMessage() == null ? failure.toString() : failure.getMessage())), "isError", true));
        }
    }

    private JsonNode request(JsonNode operation) throws Exception {
        JsonNode session = AutomationBridge.JSON.readTree(sessionFile.toFile());
        URI uri = URI.create(session.path("url").asText());
        if (!"http".equals(uri.getScheme()) || !"127.0.0.1".equals(uri.getHost()) || !"/automation".equals(uri.getPath())) throw new IllegalArgumentException("Session endpoint must be the local engine bridge");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + session.path("token").asText()).POST(HttpRequest.BodyPublishers.ofString(AutomationBridge.JSON.writeValueAsString(operation))).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode body = AutomationBridge.JSON.readTree(response.body());
        if (response.statusCode() != 200) throw new IllegalStateException(body.path("error").asText("Engine request failed"));
        return body;
    }

    private static JsonNode result(JsonNode id, Object value) {
        ObjectNode response = AutomationBridge.JSON.createObjectNode().put("jsonrpc", "2.0");
        response.set("id", id); response.set("result", AutomationBridge.JSON.valueToTree(value)); return response;
    }
    private static JsonNode error(JsonNode id, int code, String message) {
        ObjectNode response = AutomationBridge.JSON.createObjectNode().put("jsonrpc", "2.0");
        response.set("id", id); response.set("error", AutomationBridge.JSON.valueToTree(Map.of("code", code, "message", message))); return response;
    }
}
