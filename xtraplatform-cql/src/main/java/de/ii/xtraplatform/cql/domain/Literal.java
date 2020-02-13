package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;

public interface Literal extends Operand, CqlNode {

    @JsonValue
    Object getValue();

    @JsonIgnore
    Class<?> getType();

    @Override
    default String toCqlText() {
        if (getType() == String.class) {
            return String.format("'%s'", ((String)getValue()).replaceAll("'", "''"));
        }
        return getValue().toString();
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
