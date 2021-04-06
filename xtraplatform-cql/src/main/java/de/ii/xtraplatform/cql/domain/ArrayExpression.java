package de.ii.xtraplatform.cql.domain;

import java.util.Optional;

public interface ArrayExpression {

    Optional<AContains> getAContains();

    Optional<AEquals> getAEquals();

    Optional<AOverlaps> getAOverlaps();

    Optional<ContainedBy> getContainedBy();
}
