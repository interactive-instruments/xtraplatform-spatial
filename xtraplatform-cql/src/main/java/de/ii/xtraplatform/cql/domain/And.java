package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = And.class)
public interface And extends LogicalOperation, CqlNode {

    @JsonCreator
    static And of(List<CqlPredicate> predicates) {
        return new ImmutableAnd.Builder().predicates(predicates).build();
    }

    @JsonCreator
    static And of(@JsonProperty("isTopLevel") boolean isTopLevel, @JsonProperty("predicates") List<CqlPredicate> predicates) {
        return new ImmutableAnd.Builder().predicates(predicates).isTopLevel(isTopLevel).build();
    }

    @Override
    default String toCqlText() {
        return LogicalOperation.super.toCqlText("AND");
    }
}
