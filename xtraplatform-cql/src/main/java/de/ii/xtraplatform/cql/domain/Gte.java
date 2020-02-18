package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableGte.Builder.class)
public interface Gte extends ScalarOperation, CqlNode {

    abstract class Builder extends ScalarOperation.Builder<Gte> {
    }

}
