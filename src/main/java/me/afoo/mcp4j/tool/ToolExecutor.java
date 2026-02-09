package me.afoo.mcp4j.tool;

import me.afoo.mcp4j.protocol.mcp.ToolsCallResult;
import me.afoo.mcp4j.schema.SchemaValidator;

import java.util.Collections;
import java.util.Map;

/**
 * Executes MCP tools with validation and error handling.
 *
 * Distinguishes between:
 * - Protocol errors (unknown tool, invalid params) → thrown as exceptions for JSON-RPC error response
 * - Tool execution errors (business logic failures) → returned as isError=true in ToolsCallResult
 */
public class ToolExecutor {
    private final ToolRegistry registry;

    public ToolExecutor(ToolRegistry registry) {
        this.registry = registry;
    }

    /**
     * @throws ToolNotFoundException if tool name is not registered
     * @throws InvalidToolArgumentsException if arguments fail schema validation
     */
    public ToolsCallResult execute(String toolName, Map<String, Object> arguments) {
        Tool tool = registry.getTool(toolName);

        if (tool == null) {
            throw new ToolNotFoundException("Unknown tool: " + toolName);
        }

        Map<String, Object> args = arguments != null ? arguments : Collections.emptyMap();

        if (tool.getInputSchema() != null) {
            SchemaValidator.ValidationResult validation =
                    SchemaValidator.validate(tool.getInputSchema(), args);
            if (!validation.isValid()) {
                throw new InvalidToolArgumentsException(
                        "Invalid arguments: " + String.join(", ", validation.getErrors()));
            }
        }

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

    public static class ToolNotFoundException extends RuntimeException {
        public ToolNotFoundException(String message) { super(message); }
    }

    public static class InvalidToolArgumentsException extends RuntimeException {
        public InvalidToolArgumentsException(String message) { super(message); }
    }
}
