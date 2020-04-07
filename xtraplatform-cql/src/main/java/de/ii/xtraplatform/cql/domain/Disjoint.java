package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableDisjoint.Builder.class)
public interface Disjoint extends SpatialOperation, CqlNode {

    static Disjoint of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableDisjoint.Builder().property(property)
                                              .value(spatialLiteral)
                                              .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Disjoint> {
    }

}
