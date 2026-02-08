package me.afoo.mcp4j.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing MCP tools.
 */
public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public void register(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        tools.put(tool.getName(), tool);
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public void clear() {
        tools.clear();
    }
}
