package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBegins.Builder.class)
public interface Begins extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<Begins> {
    }

}
