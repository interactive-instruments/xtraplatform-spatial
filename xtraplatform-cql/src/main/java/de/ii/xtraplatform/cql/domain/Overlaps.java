package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableOverlaps.Builder.class)
public interface Overlaps extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Overlaps> {
    }

    @Override
    default String toCqlText() {
        return SpatialOperation.super.toCqlText("OVERLAPS");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
