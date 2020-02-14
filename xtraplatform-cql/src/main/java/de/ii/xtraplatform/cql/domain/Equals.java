package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableEquals.Builder.class)
public interface Equals extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Equals> {
    }

    @Override
    default String toCqlText() {
        return SpatialOperation.super.toCqlText("EQUALS");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
