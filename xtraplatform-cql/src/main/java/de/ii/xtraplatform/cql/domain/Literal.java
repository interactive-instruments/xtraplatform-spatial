package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

public interface Literal extends Operand, CqlNode {

    @JsonValue
    Object getValue();

    @JsonIgnore
    Class<?> getType();

}
