package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableGt.Builder.class)
public interface Gt extends ScalarOperation, CqlNode {

    abstract class Builder extends ScalarOperation.Builder<Gt> {
    }

    @Override
    default String toCqlText() {
        return ScalarOperation.super.toCqlText(">");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
