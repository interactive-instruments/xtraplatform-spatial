package de.ii.xtraplatform.cql.domain;

import com.google.common.collect.ImmutableList;

public interface CqlNode {

    default <T> T accept(CqlVisitor<T> visitor) {
        return visitor.visit(this, ImmutableList.of());
    }

}
