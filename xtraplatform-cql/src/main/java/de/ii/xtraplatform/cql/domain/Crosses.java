package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCrosses.Builder.class)
public interface Crosses extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Crosses> {
    }

    @Override
    default String toCqlText() {
        return SpatialOperation.super.toCqlText("CROSSES");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
