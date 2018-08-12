package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import org.immutables.value.Value;

import java.util.Map;

/**
 * @author zahnen
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableSourcePathMapping.class)
public abstract class SourcePathMapping {

    @JsonAnyGetter
    public abstract Map<String, TargetMapping> getMappings();

    public boolean hasMappingForType(String type) {
        return getMappings().containsKey(type);
    }

    public TargetMapping getMappingForType(String type) {
        return getMappings().get(type);
    }
}
