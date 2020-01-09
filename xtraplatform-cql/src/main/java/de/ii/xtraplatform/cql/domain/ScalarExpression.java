package de.ii.xtraplatform.cql.domain;

import java.util.Optional;

public interface ScalarExpression {

    //TODO: implement missing operations, see Eq and Gt

    Optional<Eq> getEq();

    //Optional<Lt> getLt();

    Optional<Gt> getGt();

    //Optional<Lte> getLte();

    //Optional<Gte> getGte();

    //Optional<Between> getBetween();

    //Optional<Like> getLike();

    //Optional<In> getIn();
}
