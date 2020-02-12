package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableEnds.Builder.class)
public interface Ends extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<Ends> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("ENDS");
    }

}
