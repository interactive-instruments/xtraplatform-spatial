package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = Or.class)
public interface Or extends LogicalOperation, CqlNode {

    @JsonCreator
    static Or of(List<CqlPredicate> predicates) {
        return new ImmutableOr.Builder()
                .predicates(predicates)
                .build();
    }

    @JsonCreator
    static Or of(@JsonProperty("isTopLevel") boolean isTopLevel, @JsonProperty("predicates") List<CqlPredicate> predicates) {
        return new ImmutableOr.Builder()
                .predicates(predicates)
                .isTopLevel(isTopLevel)
                .build();
    }

    @Override
    default String toCqlText() {
        return LogicalOperation.super.toCqlText("OR");
    }
}
