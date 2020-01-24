package de.ii.xtraplatform.cql.domain;

import java.util.Optional;

public interface ScalarExpression {

    Optional<Eq> getEq();

    Optional<Lt> getLt();

    Optional<Gt> getGt();

    Optional<Lte> getLte();

    Optional<Gte> getGte();

    Optional<Neq> getNeq();

    Optional<Between> getBetween();

    Optional<Like> getLike();

}
