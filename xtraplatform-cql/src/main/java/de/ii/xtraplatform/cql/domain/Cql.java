package de.ii.xtraplatform.cql.domain;

public interface Cql {

    enum Format {TEXT, JSON}

    CqlFilter read(String cql, Format format) throws CqlParseException;

    String write(CqlFilter cql, Format format);

}
