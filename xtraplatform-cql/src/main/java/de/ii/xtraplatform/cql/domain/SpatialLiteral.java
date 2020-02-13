package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = SpatialLiteral.Builder.class)
public interface SpatialLiteral extends Spatial, Literal, CqlNode {

    static SpatialLiteral of(String literal) throws CqlParseException {
        return new SpatialLiteral.Builder(literal).build();
    }

    static SpatialLiteral of(Geometry<?> literal) {
        return new SpatialLiteral.Builder(literal).build();
    }

    class Builder extends ImmutableSpatialLiteral.Builder {
        public Builder() {
            super();
        }

        @JsonCreator
        public Builder(Geometry<?> literal) {
            super();
            value(literal);
            type(Geometry.class);
        }

        @JsonCreator
        public Builder(String literal) throws CqlParseException {
            super();
            value(literal);
            type(String.class);
        }
    }

    @Override
    default String toCqlText() {
        return ((CqlNode)getValue()).toCqlText();
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
