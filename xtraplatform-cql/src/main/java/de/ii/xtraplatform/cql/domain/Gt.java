package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
}
