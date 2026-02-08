package me.afoo.mcp4j.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder for JSON Schema definitions used in MCP tool input validation.
 */
public class JsonSchema {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ObjectNode schema;
    private final List<String> required;

    private JsonSchema(String type) {
        this.schema = MAPPER.createObjectNode();
        this.schema.put("type", type);
        this.required = new ArrayList<>();
    }

    public static JsonSchema object() {
        return new JsonSchema("object");
    }

    public static JsonSchema string() {
        return new JsonSchema("string");
    }

    public static JsonSchema number() {
        return new JsonSchema("number");
    }

    public static JsonSchema integer() {
        return new JsonSchema("integer");
    }

    public static JsonSchema bool() {
        return new JsonSchema("boolean");
    }

    public static JsonSchema array() {
        return new JsonSchema("array");
    }

    public JsonSchema description(String description) {
        schema.put("description", description);
        return this;
    }

    public JsonSchema property(String name, JsonSchema propertySchema) {
        if (!schema.has("properties")) {
            schema.set("properties", MAPPER.createObjectNode());
        }
        ((ObjectNode) schema.get("properties")).set(name, propertySchema.build());
        if (propertySchema.required.contains("_self_")) {
            required.add(name);
        }
        return this;
    }

    public JsonSchema required() {
        this.required.add("_self_");
        return this;
    }

    public JsonSchema enumValues(String... values) {
        ArrayNode enumArray = MAPPER.createArrayNode();
        for (String value : values) {
            enumArray.add(value);
        }
        schema.set("enum", enumArray);
        return this;
    }

    public JsonSchema defaultValue(Object value) {
        if (value instanceof String) {
            schema.put("default", (String) value);
        } else if (value instanceof Number) {
            schema.put("default", ((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            schema.put("default", (Boolean) value);
        }
        return this;
    }

    public JsonSchema items(JsonSchema itemSchema) {
        schema.set("items", itemSchema.build());
        return this;
    }

    public JsonSchema minLength(int min) {
        schema.put("minLength", min);
        return this;
    }

    public JsonSchema maxLength(int max) {
        schema.put("maxLength", max);
        return this;
    }

    public JsonSchema minimum(double min) {
        schema.put("minimum", min);
        return this;
    }

    public JsonSchema maximum(double max) {
        schema.put("maximum", max);
        return this;
    }

    public JsonNode build() {
        if (!required.isEmpty() && !required.contains("_self_")) {
            ArrayNode requiredArray = MAPPER.createArrayNode();
            for (String field : required) {
                requiredArray.add(field);
            }
            schema.set("required", requiredArray);
        }
        return schema;
    }
}
