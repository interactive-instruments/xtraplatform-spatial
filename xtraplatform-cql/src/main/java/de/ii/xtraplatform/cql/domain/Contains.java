package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableContains.Builder.class)
public interface Contains extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Contains> {
    }

    @Override
    default String toCqlText() {
        return SpatialOperation.super.toCqlText("CONTAINS");
    }
}
