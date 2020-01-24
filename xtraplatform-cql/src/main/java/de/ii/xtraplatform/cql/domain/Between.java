package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBetween.Builder.class)
public interface Between extends ScalarOperation, CqlNode {

    abstract class Builder extends ScalarOperation.Builder<Between> {
    }

    @Override
    default String toCqlText() {
        return ScalarOperation.super.toCqlText("BETWEEN");
    }
}
