package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ScalarLiteral.Builder.class)
public interface ScalarLiteral extends Scalar, Literal, CqlNode {

    static ScalarLiteral of(Double literal) {
        return new Builder(literal).build();
    }

    static ScalarLiteral of(Long literal) {
        return new Builder(literal).build();
    }

    static ScalarLiteral of(Boolean literal) {
        return new Builder(literal).build();
    }

    static ScalarLiteral of(String literal) {
        return new Builder(literal).build();
    }

    static ScalarLiteral of(String literal, boolean determineType) {
        return new Builder(literal, determineType).build();
    }

    class Builder extends ImmutableScalarLiteral.Builder {
        public Builder() {
            super();
        }

        public Builder(Double literal) {
            super();
            value(literal);
            type(Double.class);
        }

        public Builder(Long literal) {
            super();
            value(literal);
            type(Long.class);
        }

        public Builder(Boolean literal) {
            super();
            value(literal);
            type(Boolean.class);
        }

        public Builder(String literal) {
            this(literal, false);
        }

        public Builder(String literal, boolean determineType) {
            super();
            if (determineType) {
                Object castedLiteral = castToType(literal);
                value(castedLiteral);
                type(castedLiteral.getClass());
            } else {
                value(literal);
                type(String.class);
            }
        }

        private Object castToType(String literal) {
            try {
                return Long.valueOf(literal);
            } catch (NumberFormatException e) {
                try {
                    return Double.valueOf(literal);
                } catch (NumberFormatException e2) {
                    if (literal.equalsIgnoreCase("true") || literal.equalsIgnoreCase("false")) {
                        return Boolean.valueOf(literal);
                    }
                }
            }

            return literal;
        }
    }


    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
