package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;

public interface LogicalOperation {

    @JsonValue
    List<CqlPredicate> getPredicates();

    @Value.Check
    default void check() {
        Preconditions.checkState(getPredicates().size() > 1, "a boolean operation must have at least two children, found %s", getPredicates().size());
    }

    default String toCqlText(String operator) {
        return getPredicates().stream()
                .map(CqlPredicate::toCqlText)
                .collect(Collectors.joining(String.format(" %s ", operator), "(", ")"));
    }

    default String toCqlTextTopLevel(String operator) {
        return getPredicates().stream()
                .map(CqlPredicate::toCqlText)
                .collect(Collectors.joining(String.format(" %s ", operator)));
    }
}
