package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Optional annotations describing tool behavior hints.
 * All fields are hints and not guaranteed to be accurate.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolAnnotations {

    @JsonProperty("readOnlyHint")
    private Boolean readOnlyHint;

    @JsonProperty("destructiveHint")
    private Boolean destructiveHint;

    @JsonProperty("idempotentHint")
    private Boolean idempotentHint;

    @JsonProperty("openWorldHint")
    private Boolean openWorldHint;

    public ToolAnnotations() {}

    public Boolean getReadOnlyHint() { return readOnlyHint; }
    public void setReadOnlyHint(Boolean readOnlyHint) { this.readOnlyHint = readOnlyHint; }

    public Boolean getDestructiveHint() { return destructiveHint; }
    public void setDestructiveHint(Boolean destructiveHint) { this.destructiveHint = destructiveHint; }

    public Boolean getIdempotentHint() { return idempotentHint; }
    public void setIdempotentHint(Boolean idempotentHint) { this.idempotentHint = idempotentHint; }

    public Boolean getOpenWorldHint() { return openWorldHint; }
    public void setOpenWorldHint(Boolean openWorldHint) { this.openWorldHint = openWorldHint; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ToolAnnotations annotations = new ToolAnnotations();

        public Builder readOnly(boolean value) { annotations.readOnlyHint = value; return this; }
        public Builder destructive(boolean value) { annotations.destructiveHint = value; return this; }
        public Builder idempotent(boolean value) { annotations.idempotentHint = value; return this; }
        public Builder openWorld(boolean value) { annotations.openWorldHint = value; return this; }

        public ToolAnnotations build() { return annotations; }
    }
}
