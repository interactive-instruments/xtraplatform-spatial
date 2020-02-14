package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableWithin.Builder.class)
public interface Within extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Within> {
    }

    @Override
    default String toCqlText() {
        return SpatialOperation.super.toCqlText("WITHIN");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
