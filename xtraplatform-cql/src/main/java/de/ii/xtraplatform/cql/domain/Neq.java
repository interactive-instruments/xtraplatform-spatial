package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableNeq.Builder.class)
public interface Neq extends ScalarOperation, CqlNode {

    abstract class Builder extends ScalarOperation.Builder<Neq> {
    }

    @Override
    default String toCqlText() {
        return ScalarOperation.super.toCqlText("<>");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
