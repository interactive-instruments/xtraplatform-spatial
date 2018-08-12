package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableTargetMappingTestMicrodata.class)
public abstract class TargetMappingTestMicrodata implements TargetMapping<TargetMappingTestMicrodata.WFS3_TYPES> {
    @Override
    public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
        return targetMapping;
    }

    @Value.Derived
    @Override
    public boolean isSpatial() {
        return getType() == WFS3_TYPES.SPATIAL;
    }

    public enum WFS3_TYPES {ID,VALUE,SPATIAL,TEMPORAL,STRING,NUMBER,BOOLEAN,DATE,GEOMETRY,REFERENCE}

}
