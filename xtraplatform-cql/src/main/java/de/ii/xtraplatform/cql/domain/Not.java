package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = Not.class)
public interface Not extends LogicalOperation, CqlNode {

    @JsonCreator
    static Not of(List<CqlPredicate> predicates) {
        return new ImmutableNot.Builder()
                .predicates(predicates)
                .build();
    }

    static Not of(BinaryOperation<?> binaryOperation) {
        return new ImmutableNot.Builder()
                .addPredicates(CqlPredicate.of(binaryOperation))
                .build();
    }

    @Value.Check
    @Override
    default void check() {
        Preconditions.checkState(getPredicates().size() == 1, "a NOT operation must have one child, found %s",
                getPredicates().size());
    }

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        T expression = getPredicates()
                .get(0)
                .accept(visitor);

        return visitor.visit(this, Lists.newArrayList(expression));
    }
}
