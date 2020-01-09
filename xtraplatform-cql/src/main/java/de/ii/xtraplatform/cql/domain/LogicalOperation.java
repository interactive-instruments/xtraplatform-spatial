package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.List;

public interface LogicalOperation {

    @JsonValue
    List<CqlPredicate> getPredicates();

    @Value.Check
    default void check() {
        Preconditions.checkState(getPredicates().size() == 2, "a boolean operation must have exactly two children, found %s", getPredicates().size());
    }

    default String toCqlText(String operator) {
        return String.format("%s %s %s", getPredicates().get(0).toCqlText(), operator, getPredicates().get(1).toCqlText());
    }
}
