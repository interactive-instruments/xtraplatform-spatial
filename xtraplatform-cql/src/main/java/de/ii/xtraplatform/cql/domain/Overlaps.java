package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableOverlaps.Builder.class)
public interface Overlaps extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Overlaps> {
    }

}
