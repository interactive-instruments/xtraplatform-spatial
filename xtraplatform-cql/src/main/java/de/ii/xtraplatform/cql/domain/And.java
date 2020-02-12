package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.CqlObjectVisitor;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
@JsonDeserialize(as = And.class)
public interface And extends LogicalOperation, CqlNode {

    @JsonCreator
    static And of(List<CqlPredicate> predicates) {
        return new ImmutableAnd.Builder().predicates(predicates).build();
    }

    static And of(CqlPredicate... predicates) {
        return new ImmutableAnd.Builder().predicates(Arrays.asList(predicates)).build();
    }

    @Override
    default String toCqlText() {
        return LogicalOperation.super.toCqlText("AND");
    }

    @Override
    default String toCqlTextTopLevel() {
        return LogicalOperation.super.toCqlTextTopLevel("AND");
    }

    @Override
    default <T> T accept(CqlObjectVisitor<T> visitor) {
        List<T> children = getPredicates().stream()
                .map(predicate -> predicate.accept(visitor))
                .collect(Collectors.toList());
        return visitor.visit(this, children);
    }

}
