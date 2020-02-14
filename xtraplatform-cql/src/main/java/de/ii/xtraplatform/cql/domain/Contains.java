package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
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

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
