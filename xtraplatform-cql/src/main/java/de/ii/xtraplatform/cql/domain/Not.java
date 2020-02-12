package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = Not.class)
public interface Not extends LogicalOperation, CqlNode {

    @Value.Check
    @Override
    default void check() {
        Preconditions.checkState(getPredicates().size() == 1, "a NOT operation must have one child, found %s",
                getPredicates().size());
    }

    @JsonCreator
    static Not of(List<CqlPredicate> predicates) {
        return new ImmutableNot.Builder()
                .predicates(predicates)
                .build();
    }

    @Override
    default String toCqlText() {
        return getPredicates()
                .get(0)
                .getExpressions()
                .get(0)
                .toCqlTextNot();
    }
}
