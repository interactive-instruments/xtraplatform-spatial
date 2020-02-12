package de.ii.xtraplatform.cql.domain;

public interface Cql {

    enum Format {TEXT, JSON}

    CqlPredicate read(String cql, Format format) throws CqlParseException;

    String write(CqlPredicate cql, Format format);

}
