package de.ii.xtraplatform.cql.domain;

import de.ii.xtraplatform.cql.infra.ObjectVisitor;

public interface Operand extends CqlNode {

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
