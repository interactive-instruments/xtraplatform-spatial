package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.CqlObjectVisitor;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
@JsonDeserialize(as = Or.class)
public interface Or extends LogicalOperation, CqlNode {

    @JsonCreator
    static Or of(List<CqlPredicate> predicates) {
        return new ImmutableOr.Builder()
                .predicates(predicates)
                .build();
    }

    @Override
    default String toCqlText() {
        return LogicalOperation.super.toCqlText("OR");
    }

    @Override
    default String toCqlTextTopLevel() {
        return LogicalOperation.super.toCqlTextTopLevel("OR");
    }

    @Override
    default <T> T accept(CqlObjectVisitor<T> visitor) {
        List<T> children = getPredicates().stream()
                .map(predicate -> predicate.accept(visitor))
                .collect(Collectors.toList());
        return visitor.visit(this, children);
    }
}
