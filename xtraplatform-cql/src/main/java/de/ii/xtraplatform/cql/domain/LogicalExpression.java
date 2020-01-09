package de.ii.xtraplatform.cql.domain;

import java.util.Optional;

public interface LogicalExpression {

    Optional<And> getAnd();

    //TODO
    //Optional<Or> getOr();

    //TODO
    //Optional<Not> getNot();

}
