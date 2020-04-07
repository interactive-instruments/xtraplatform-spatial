package de.ii.xtraplatform.cql.domain;

import java.util.Optional;

public interface LogicalExpression {

    Optional<And> getAnd();

    Optional<Or> getOr();

    Optional<Not> getNot();

}
