package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableWithin.Builder.class)
public interface Within extends SpatialOperation, CqlNode {

    static Within of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableWithin.Builder().property(property)
                                            .value(spatialLiteral)
                                            .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Within> {
    }

}
