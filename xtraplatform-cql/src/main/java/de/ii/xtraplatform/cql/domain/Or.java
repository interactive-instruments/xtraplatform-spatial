package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
@JsonDeserialize(as = Or.class)
public interface Or extends LogicalOperation, CqlNode {

    static Or of(CqlPredicate... predicates) {
        return new ImmutableOr.Builder()
                .addPredicates(predicates)
                .build();
    }

    @JsonCreator
    static Or of(List<CqlPredicate> predicates) {
        return new ImmutableOr.Builder()
                .predicates(predicates)
                .build();
    }

}
