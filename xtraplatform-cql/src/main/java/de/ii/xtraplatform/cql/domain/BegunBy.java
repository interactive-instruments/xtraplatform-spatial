package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBegunBy.Builder.class)
public interface BegunBy extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<BegunBy> {
    }

}
