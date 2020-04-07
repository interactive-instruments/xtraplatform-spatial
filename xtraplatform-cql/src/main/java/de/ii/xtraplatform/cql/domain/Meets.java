package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableMeets.Builder.class)
public interface Meets extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<Meets> {
    }

}
