package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCrosses.Builder.class)
public interface Crosses extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Crosses> {
    }

}
