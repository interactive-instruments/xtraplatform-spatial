package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTouches.Builder.class)
public interface Touches extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Touches> {
    }

    @Override
    default String toCqlText() {
        return SpatialOperation.super.toCqlText("TOUCHES");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
