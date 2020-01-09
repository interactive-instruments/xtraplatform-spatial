package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableEq.Builder.class)
public interface Eq extends ScalarOperation, CqlNode {

    abstract class Builder extends ScalarOperation.Builder<Eq> {
    }

    @Override
    default String toCqlText() {
        return ScalarOperation.super.toCqlText("=");
    }
}
