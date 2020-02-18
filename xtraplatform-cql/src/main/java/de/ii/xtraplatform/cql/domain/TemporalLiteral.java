package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Joiner;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@Value.Immutable
@JsonDeserialize(builder = TemporalLiteral.Builder.class)
public interface TemporalLiteral extends Temporal, Literal, CqlNode {

    static TemporalLiteral of(Instant literal) {
        return new TemporalLiteral.Builder(literal).build();
    }

    static TemporalLiteral of(Interval literal) {
        return new TemporalLiteral.Builder(literal).build();
    }

    static TemporalLiteral of(List<String> literal) throws CqlParseException {
        return new TemporalLiteral.Builder(literal).build();
    }

    static TemporalLiteral of(String literal) throws CqlParseException {
        return new TemporalLiteral.Builder(literal).build();
    }

    Joiner INTERVAL_JOINER = Joiner.on('/').skipNulls();

    class Builder extends ImmutableTemporalLiteral.Builder {
        public Builder() {
            super();
        }

        public Builder(Instant literal) {
            super();
            value(literal);
            type(Instant.class);
        }

        public Builder(Interval literal) {
            super();
            value(literal);
            type(Interval.class);
        }

        @JsonCreator
        public Builder(List<String> literal) throws CqlParseException {
            this(INTERVAL_JOINER.join(literal));
        }

        @JsonCreator
        public Builder(String literal) throws CqlParseException {
            super();
            Object castedLiteral = castToType(literal);
            value(castedLiteral);
            type(castedLiteral.getClass());
        }

        private Object castToType(String literal) throws CqlParseException {
            try {
                return Interval.parse(literal);
            } catch (DateTimeParseException e) {
                try {
                    return Instant.parse(literal);
                } catch (DateTimeParseException e2) {
                    //ignore
                }
            }

            throw new CqlParseException("not a valid temporal literal: " + literal);
        }
    }


}
