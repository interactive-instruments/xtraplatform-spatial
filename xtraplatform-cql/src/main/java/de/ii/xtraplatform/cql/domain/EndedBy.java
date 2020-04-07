package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableEndedBy.Builder.class)
public interface EndedBy extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<EndedBy> {
    }

}
