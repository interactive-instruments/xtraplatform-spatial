package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableMeets.Builder.class)
public interface Meets extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<Meets> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("MEETS");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
