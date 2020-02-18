package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTContains.Builder.class)
public interface TContains extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<TContains> {
    }

}
