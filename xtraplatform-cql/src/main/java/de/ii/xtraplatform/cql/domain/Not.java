package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
@JsonDeserialize(as = Not.class)
public interface Not extends LogicalOperation, CqlNode {

@Override
    default void check() {
        Preconditions.checkState(getPredicates().size() == 1, "a NOT operation must have exactly one children, found %s", getPredicates().size());
    }

    @JsonCreator
    static Not of(List<CqlPredicate> predicates) {
        return new ImmutableNot.Builder().predicates(predicates).build();
    }

    @Override
    default String toCqlText() {
        CqlPredicate predicate = getPredicates().get(0);
        if (predicate.getLike().isPresent()) {
            return String.format("%s NOT LIKE %s",
                    predicate.getLike()
                            .get()
                            .getProperty()
                            .get()
                            .toCqlText(),
                    predicate.getLike()
                            .get()
                            .getValue()
                            .get()
                            .toCqlText());
        }
        return String.format("%s (%s)", "NOT", predicate.toCqlText());
    }
}
