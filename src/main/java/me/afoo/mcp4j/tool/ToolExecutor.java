package me.afoo.mcp4j.tool;

import me.afoo.mcp4j.protocol.mcp.ToolsCallResult;
import me.afoo.mcp4j.schema.SchemaValidator;

import java.util.Collections;
import java.util.Map;

/**
 * Executes MCP tools with validation and error handling.
 */
public class ToolExecutor {
    private final ToolRegistry registry;

    public ToolExecutor(ToolRegistry registry) {
        this.registry = registry;
    }

    public ToolsCallResult execute(String toolName, Map<String, Object> arguments) {
        Tool tool = registry.getTool(toolName);
        
        if (tool == null) {
            return createErrorResult("Tool not found: " + toolName);
        }

        Map<String, Object> args = arguments != null ? arguments : Collections.emptyMap();

        // Validate input against schema
        if (tool.getInputSchema() != null) {
            SchemaValidator.ValidationResult validation = 
                SchemaValidator.validate(tool.getInputSchema(), args);
            
            if (!validation.isValid()) {
                return createErrorResult("Invalid arguments: " + String.join(", ", validation.getErrors()));
            }
        }

        // Execute tool
        try {
            Object result = tool.getHandler().execute(args);
            return createSuccessResult(result);
        } catch (Exception e) {
            return createErrorResult("Tool execution failed: " + e.getMessage());
        }
    }

    private ToolsCallResult createSuccessResult(Object result) {
        String text = result != null ? result.toString() : "";
        ToolsCallResult.Content content = ToolsCallResult.Content.text(text);
        return new ToolsCallResult(Collections.singletonList(content));
    }

    private ToolsCallResult createErrorResult(String message) {
        ToolsCallResult result = new ToolsCallResult();
        ToolsCallResult.Content content = ToolsCallResult.Content.text(message);
        result.setContent(Collections.singletonList(content));
        result.setIsError(true);
        return result;
    }
}
