package me.afoo.mcp4j.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Validates JSON data against JSON Schema.
 */
public class SchemaValidator {

    public static ValidationResult validate(JsonNode schema, Map<String, Object> data) {
        List<String> errors = new ArrayList<>();
        
        if (schema == null) {
            return new ValidationResult(true, errors);
        }

        String type = schema.has("type") ? schema.get("type").asText() : null;
        
        if ("object".equals(type)) {
            validateObject(schema, data, errors);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private static void validateObject(JsonNode schema, Map<String, Object> data, List<String> errors) {
        // Check required fields
        if (schema.has("required")) {
            JsonNode required = schema.get("required");
            if (required.isArray()) {
                for (JsonNode field : required) {
                    String fieldName = field.asText();
                    if (!data.containsKey(fieldName)) {
                        errors.add("Missing required field: " + fieldName);
                    }
                }
            }
        }

        // Validate properties
        if (schema.has("properties")) {
            JsonNode properties = schema.get("properties");
            Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey();
                JsonNode fieldSchema = entry.getValue();
                
                if (data.containsKey(fieldName)) {
                    Object value = data.get(fieldName);
                    validateField(fieldName, fieldSchema, value, errors);
                }
            }
        }
    }

    private static void validateField(String fieldName, JsonNode fieldSchema, Object value, List<String> errors) {
        if (!fieldSchema.has("type")) {
            return;
        }

        String type = fieldSchema.get("type").asText();
        
        switch (type) {
            case "string":
                if (!(value instanceof String)) {
                    errors.add("Field '" + fieldName + "' must be a string");
                }
                break;
            case "number":
                if (!(value instanceof Number)) {
                    errors.add("Field '" + fieldName + "' must be a number");
                }
                break;
            case "integer":
                if (!(value instanceof Integer) && !(value instanceof Long)) {
                    errors.add("Field '" + fieldName + "' must be an integer");
                }
                break;
            case "boolean":
                if (!(value instanceof Boolean)) {
                    errors.add("Field '" + fieldName + "' must be a boolean");
                }
                break;
        }

        // Check enum values
        if (fieldSchema.has("enum") && value instanceof String) {
            boolean found = false;
            for (JsonNode enumValue : fieldSchema.get("enum")) {
                if (enumValue.asText().equals(value)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                errors.add("Field '" + fieldName + "' has invalid enum value");
            }
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
