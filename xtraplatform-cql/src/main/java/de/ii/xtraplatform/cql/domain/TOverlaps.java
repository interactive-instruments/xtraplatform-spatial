package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTOverlaps.Builder.class)
public interface TOverlaps extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<TOverlaps> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("TOVERLAPS");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
