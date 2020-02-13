package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(of = "new")
@JsonDeserialize(as = ImmutableProperty.class)
public interface Property extends Scalar, Spatial, Temporal, Operand, CqlNode {

    static Property of(String name) {
        return new ImmutableProperty(name);
    }

    @JsonValue
    @Value.Parameter
    String getName();

    Splitter PATH_SPLITTER = Splitter.on('.')
                                     .omitEmptyStrings();

    @JsonIgnore
    @Value.Derived
    default List<String> getPath() {
        return PATH_SPLITTER.splitToList(getName());
    }

    @Override
    default String toCqlText() {
        return getName();
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
