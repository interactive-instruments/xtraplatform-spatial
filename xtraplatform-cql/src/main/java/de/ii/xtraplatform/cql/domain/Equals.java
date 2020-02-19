package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableEquals.Builder.class)
public interface Equals extends SpatialOperation, CqlNode {

    static Equals of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableEquals.Builder().property(property)
                                            .value(spatialLiteral)
                                            .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Equals> {
    }

}
