package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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

}
