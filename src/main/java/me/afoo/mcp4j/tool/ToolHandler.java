package me.afoo.mcp4j.tool;

import java.util.Map;

/**
 * Functional interface for tool execution handlers.
 */
@FunctionalInterface
public interface ToolHandler {
    /**
     * Execute the tool with given arguments.
     *
     * @param arguments Tool input arguments
     * @return Tool execution result
     * @throws Exception if execution fails
     */
    Object execute(Map<String, Object> arguments) throws Exception;
}
