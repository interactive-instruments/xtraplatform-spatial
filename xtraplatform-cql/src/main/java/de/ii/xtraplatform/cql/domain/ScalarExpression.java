package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface ScalarExpression {

    Optional<Eq> getEq();

    Optional<Lt> getLt();

    Optional<Gt> getGt();

    Optional<Lte> getLte();

    Optional<Gte> getGte();

    Optional<Neq> getNeq();

    Optional<Between> getBetween();

    // getter method for the IN operator was changed to avoid deserialization errors due to ambiguity with getWithin()
    @JsonProperty("in")
    Optional<In> getInOperator();

    Optional<Like> getLike();

    Optional<IsNull> getIsNull();

    Optional<Exists> getExists();

}
