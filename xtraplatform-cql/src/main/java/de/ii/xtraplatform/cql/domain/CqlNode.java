package de.ii.xtraplatform.cql.domain;

public interface CqlNode {

    default String toCqlText() {
        return "";
    }
}
