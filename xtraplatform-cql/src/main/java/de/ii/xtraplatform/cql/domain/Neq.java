package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
}
