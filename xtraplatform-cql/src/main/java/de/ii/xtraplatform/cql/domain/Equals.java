package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableEquals.Builder.class)
public interface Equals extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Equals> {
    }

}
