package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableMetBy.Builder.class)
public interface MetBy extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<MetBy> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("METBY");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
