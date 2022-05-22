/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Value.Immutable
@JsonDeserialize(builder = TemporalLiteral.Builder.class)
public interface TemporalLiteral extends Temporal, Scalar, Literal, CqlNode {

    @JsonIgnore
    @JsonValue(false)
    @Override
    Object getValue();

    @Value.Derived
    default Optional<LocalDate> getDate() {
        return getType() == LocalDate.class
            ? Optional.ofNullable((LocalDate) getValue())
            : Optional.empty();
    }

    @Value.Derived
    default Optional<Instant> getTimestamp() {
        return getType() == Instant.class
            ? Optional.ofNullable((Instant) getValue())
            : Optional.empty();
    }

    @Value.Derived
    default Optional<Interval> getInterval() {
        return getType() == Interval.class
            ? Optional.ofNullable((Interval) getValue())
            : Optional.empty();
    }

    enum OPEN { OPEN }

    String DATE_REGEX = "(?:[0-9]+)-(?:0[1-9]|1[012])-(?:0[1-9]|[12][0-9]|3[01])";
    String TIMESTAMP_REGEX = DATE_REGEX + "T(?:[01][0-9]|2[0-3]):(?:[0-5][0-9]):(?:[0-5][0-9]|60)(?:\\.[0-9]+)?Z";
    Predicate<String> TIMESTAMP_PATTERN = Pattern.compile(String.format("^%s$", TIMESTAMP_REGEX))
        .asPredicate();
    Predicate<String> DATE_PATTERN = Pattern.compile(String.format("^%s$", DATE_REGEX))
        .asPredicate();

    static TemporalLiteral of(Instant instant) {
        return new Builder(instant).build();
    }

    static TemporalLiteral of(String startInclusive, String endInclusive) {
        return new Builder(TemporalLiteral.of(startInclusive), TemporalLiteral.of(endInclusive)).build();
    }

    static TemporalLiteral of(TemporalLiteral startInclusive, TemporalLiteral endInclusive) {
        return new Builder(startInclusive, endInclusive).build();
    }

    static TemporalLiteral of(List<String> startEndInclusive) {
        assert startEndInclusive.size()>=2;
        return new Builder(TemporalLiteral.of(startEndInclusive.get(0)), TemporalLiteral.of(startEndInclusive.get(1))).build();
    }

    static TemporalLiteral of(Instant startInclusive, Instant endExclusive) {
        return new Builder(startInclusive, endExclusive).build();
    }

    static TemporalLiteral of(String instantLiteral) throws CqlParseException {
        return new Builder(instantLiteral).build();
    }

    static Temporal interval(Temporal op1, Temporal op2) {
        // if at least one parameter is a property, we create a function, otherwise a fixed interval
        if (op1 instanceof Property && op2 instanceof Property) {
            return Function.of("INTERVAL", ImmutableList.of((Property) op1, (Property) op2));
        } else if (op1 instanceof Property &&  op2 instanceof TemporalLiteral) {
            return Function.of("INTERVAL", ImmutableList.of((Property) op1, (TemporalLiteral) op2));
        } else if (op1 instanceof TemporalLiteral &&  op2 instanceof Property) {
            return Function.of("INTERVAL", ImmutableList.of((TemporalLiteral) op1, (Property) op2));
        } else if (op1 instanceof TemporalLiteral &&  op2 instanceof TemporalLiteral) {
            return TemporalLiteral.of((TemporalLiteral) op1, (TemporalLiteral) op2);
        }

        throw new IllegalStateException(
            String.format("unsupported interval operands: %s, %s", op1.getClass(), op2.getClass()));
    }

    static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    class Builder extends ImmutableTemporalLiteral.Builder {
        public Builder() {
            super();
        }

        public Builder(Instant instant) {
            super();
            value(instant);
            type(Instant.class);
        }

        public Builder(Instant startInclusive, Instant endExclusive) {
            super();
            value(Interval.of(startInclusive, endExclusive));
            type(Interval.class);
        }

        public Builder(Interval interval) {
            super();
            value(interval);
            type(Interval.class);
        }

        @JsonCreator
        public Builder(String instantLiteral) throws CqlParseException {
            super();
            Object castedLiteral = castToType(instantLiteral);
            value(castedLiteral);
            type(castedLiteral.getClass());
        }

        public Builder(TemporalLiteral startInclusive, TemporalLiteral endInclusive) throws CqlParseException {
            super();
            if ((startInclusive.getType()==Instant.class || startInclusive.getType()==OPEN.class)
                && (endInclusive.getType()==Instant.class || endInclusive.getType()==OPEN.class)) {
                value(Interval.of(getStartInclusive(startInclusive.getValue()), getEndExclusive(endInclusive.getValue())));
                type(Interval.class);
            } else {
                value(Function.of("INTERVAL", ImmutableList.of(startInclusive, endInclusive)));
                type(Function.class);
            }
        }

        private Instant getStartInclusive(Object instant) {
            if (instant instanceof OPEN)
                return Instant.MIN;
            else if (instant instanceof LocalDate)
                return ((LocalDate) instant).atStartOfDay(ZoneOffset.UTC).toInstant();
            return (Instant) instant;
        }

        private Instant getEndExclusive(Object instant) {
            if (instant instanceof OPEN)
                return Instant.MAX;
            else if (instant instanceof LocalDate)
                return ((LocalDate) instant).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return ((Instant) instant);
        }

        private Object castToType(String instantLiteral) throws CqlParseException {

            // If the datetime parameter uses dates, not timestamps, the result is always an interval that
            // starts on the first second of the start date and ends at the last second of the end date.
            try {
                if (TIMESTAMP_PATTERN.test(instantLiteral.toUpperCase())) {
                    // a fully specified datetime instant
                    // Instant does not support timezones, convert to UTC
                    return ZonedDateTime.parse(instantLiteral).toInstant();
                } else if (DATE_PATTERN.test(instantLiteral.toUpperCase())) {
                    // a date only instant
                    return LocalDate.parse(instantLiteral);
                } else if (instantLiteral.equalsIgnoreCase("NOW")
                    || instantLiteral.equalsIgnoreCase("NOW()")) {
                    // now
                    return Instant.now();
                } else if (instantLiteral.equals("..")) {
                    // an open interval boundary, we do not know, if this is start or end, so we need a special value
                    return OPEN.OPEN;
                }
            } catch (DateTimeParseException e) {
                //ignore
            }

            throw new CqlParseException("not a valid instant literal: " + instantLiteral);
        }
    }
}
