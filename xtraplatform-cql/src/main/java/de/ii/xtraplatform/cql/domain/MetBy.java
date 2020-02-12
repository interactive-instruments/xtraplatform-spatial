package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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

}
