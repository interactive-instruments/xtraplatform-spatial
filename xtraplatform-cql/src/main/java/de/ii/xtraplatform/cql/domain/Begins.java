package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBegins.Builder.class)
public interface Begins extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<Begins> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("BEGINS");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
