package com.xenoamess.cyan_potion.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Loopback-only bridge. The host must call drain() on its game/render thread. */
public final class AutomationBridge implements AutoCloseable {
    public static final ObjectMapper JSON = new ObjectMapper();
    private final AutomationTarget target;
    private final HttpServer server;
    private final java.util.concurrent.ExecutorService workers;
    private final ArrayBlockingQueue<Job> jobs = new ArrayBlockingQueue<>(128);
    private final ArrayDeque<ObjectNode> events = new ArrayDeque<>();
    private final String token;
    private final Path sessionFile;
    private long sequence;
    private volatile boolean closed;

    private record Job(JsonNode request, CompletableFuture<JsonNode> result) { }

    public AutomationBridge(AutomationTarget target, Path sessionFile, int port) throws IOException {
        this.target = target;
        this.sessionFile = sessionFile.toAbsolutePath();
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        workers = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "engine-automation-http");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(workers);
        server.createContext("/automation", this::handle);
        Files.createDirectories(this.sessionFile.getParent());
        JSON.writeValue(this.sessionFile.toFile(), Map.of("url", "http://127.0.0.1:"
                + server.getAddress().getPort() + "/automation", "token", token,
                "pid", ProcessHandle.current().pid(), "startedAt", Instant.now().toString()));
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) { reply(exchange, 405, Map.of("error", "POST required")); return; }
            if (exchange.getRequestHeaders().getFirst("Origin") != null) { reply(exchange, 403, Map.of("error", "Browser origins are not accepted")); return; }
            if (!("Bearer " + token).equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                reply(exchange, 401, Map.of("error", "Invalid session token")); return;
            }
            byte[] body = exchange.getRequestBody().readNBytes(1_048_577);
            if (body.length > 1_048_576) { reply(exchange, 413, Map.of("error", "Request too large")); return; }
            JsonNode request = JSON.readTree(body);
            if (request == null || !request.isObject() || !request.path("operation").isTextual()) {
                reply(exchange, 400, Map.of("error", "An operation is required")); return;
            }
            CompletableFuture<JsonNode> result = new CompletableFuture<>();
            Job job = new Job(request, result);
            if (closed || !jobs.offer(job)) { reply(exchange, 503, Map.of("error", "Automation queue unavailable")); return; }
            try {
                reply(exchange, 200, result.get(10, TimeUnit.SECONDS));
            } catch (java.util.concurrent.TimeoutException timeout) {
                result.cancel(false);
                jobs.remove(job);
                reply(exchange, 504, Map.of("error", "Game thread did not service the request; inspect the game log"));
            } catch (Exception failure) {
                reply(exchange, 400, Map.of("error", failure.getCause() == null ? failure.toString() : failure.getCause().toString()));
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
            reply(exchange, 400, Map.of("error", "Invalid JSON"));
        } finally { exchange.close(); }
    }

    private static void reply(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    /** Call once per frame, on the thread which owns game state and the GL context. */
    public void drain() {
        for (int count = 0; count < 128; count++) {
            Job job = jobs.poll();
            if (job == null) return;
            if (job.result.isCancelled()) continue;
            try { job.result.complete(dispatch(job.request)); }
            catch (Exception failure) { job.result.completeExceptionally(failure); }
        }
    }

    private JsonNode dispatch(JsonNode request) throws Exception {
        return switch (request.path("operation").asText()) {
            case "status" -> target.state();
            case "commands" -> JSON.valueToTree(target.commands());
            case "command" -> {
                String name = request.path("name").asText();
                if (target.commands().stream().noneMatch(command -> command.name().equals(name))) {
                    throw new IllegalArgumentException("Unknown game command: " + name);
                }
                JsonNode arguments = request.path("arguments");
                if (!arguments.isObject()) throw new IllegalArgumentException("arguments must be an object");
                JsonNode result = target.execute(name, arguments);
                record(name, arguments, result);
                yield result;
            }
            case "screenshot" -> JSON.valueToTree(Map.of("mimeType", "image/png", "data", Base64.getEncoder().encodeToString(target.screenshot())));
            case "events" -> JSON.valueToTree(new ArrayList<>(events).stream()
                    .filter(event -> event.path("sequence").asLong() > request.path("after").asLong(0)).toList());
            default -> throw new IllegalArgumentException("Unknown automation operation");
        };
    }

    public void record(String name, JsonNode arguments, JsonNode result) {
        ObjectNode event = JSON.createObjectNode().put("sequence", ++sequence).put("time", Instant.now().toString()).put("command", name);
        event.set("arguments", arguments.deepCopy());
        event.set("result", result.deepCopy());
        events.addLast(event);
        while (events.size() > 256) events.removeFirst();
    }

    @Override public void close() {
        closed = true;
        server.stop(0);
        workers.shutdownNow();
        Job job;
        while ((job = jobs.poll()) != null) job.result.completeExceptionally(new IllegalStateException("Engine closed"));
        try {
            if (Files.exists(sessionFile) && JSON.readTree(sessionFile.toFile()).path("token").asText().equals(token)) Files.delete(sessionFile);
        } catch (IOException ignored) { /* A newer session may already own the file. */ }
    }
}
