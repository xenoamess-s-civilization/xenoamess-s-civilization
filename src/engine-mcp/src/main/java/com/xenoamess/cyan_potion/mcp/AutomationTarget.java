package com.xenoamess.cyan_potion.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Implemented by a game; invoked exclusively by its automation queue owner. */
public interface AutomationTarget {
    JsonNode state();
    List<Command> commands();
    JsonNode execute(String command, JsonNode arguments);
    byte[] screenshot() throws Exception;

    record Command(String name, String description, JsonNode inputSchema) { }
}
