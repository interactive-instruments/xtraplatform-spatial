package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = And.class)
public interface And extends LogicalOperation, CqlNode {

    static And of(CqlPredicate... predicates) {
        return new ImmutableAnd.Builder()
                .addPredicates(predicates)
                .build();
    }

    @JsonCreator
    static And of(List<CqlPredicate> predicates) {
        return new ImmutableAnd.Builder().predicates(predicates)
                                         .build();
    }

}
