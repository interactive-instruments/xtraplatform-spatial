/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Joiner;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Value.Immutable
@JsonDeserialize(builder = TemporalLiteral.Builder.class)
public interface TemporalLiteral extends Temporal, Literal, CqlNode {

    Instant MIN_DATE = Instant.parse("0000-01-01T00:00:00Z");
    Instant MAX_DATE = Instant.parse("9999-12-31T23:59:59Z");

    String DATE_REGEX = "([0-9]+)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])";
    String TIMESTAMP_REGEX = "([0-9]+)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])[Tt]([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9]|60)(\\.[0-9]+)?(([Zz])|([\\+|\\-]([01][0-9]|2[0-3]):[0-5][0-9]))";
    String OPEN_REGEX = "(\\.\\.)?";
    Predicate<String> INSTANT_PATTERN = Pattern.compile(String.format("^%s$", TIMESTAMP_REGEX))
                                               .asPredicate();
    Predicate<String> INTERVAL_PATTERN = Pattern.compile(String.format("^%s/%s$", TIMESTAMP_REGEX, TIMESTAMP_REGEX))
                                                .asPredicate();
    Predicate<String> INTERVAL_OPEN_START_PATTERN = Pattern.compile(String.format("^%s/%s$", OPEN_REGEX, TIMESTAMP_REGEX))
                                                           .asPredicate();
    Predicate<String> INTERVAL_OPEN_END_PATTERN = Pattern.compile(String.format("^%s/%s$", TIMESTAMP_REGEX, OPEN_REGEX))
                                                         .asPredicate();
    Predicate<String> INTERVAL_OPEN_PATTERN = Pattern.compile(String.format("^%s/%s$", OPEN_REGEX, OPEN_REGEX))
                                                     .asPredicate();
    Predicate<String> DATE_PATTERN = Pattern.compile(String.format("^%s$", DATE_REGEX))
                                            .asPredicate();
    Predicate<String> DATE_INTERVAL_PATTERN = Pattern.compile(String.format("^%s/%s$", DATE_REGEX, DATE_REGEX))
                                                .asPredicate();
    Predicate<String> DATE_INTERVAL_OPEN_START_PATTERN = Pattern.compile(String.format("^%s/%s$", OPEN_REGEX, DATE_REGEX))
                                                                .asPredicate();
    Predicate<String> DATE_INTERVAL_OPEN_END_PATTERN = Pattern.compile(String.format("^%s/%s$", DATE_REGEX, OPEN_REGEX))
                                                              .asPredicate();
    Joiner INTERVAL_JOINER = Joiner.on('/')
                                   .skipNulls();

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
            /*try {
                return Interval.parse(literal);
            } catch (DateTimeParseException e) {
                try {
                    return Instant.parse(literal);
                } catch (DateTimeParseException e2) {
                    try {
                        return LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(literal));
                    } catch (DateTimeParseException e3) {
                        //ignore
                    }
                }
            }*/

            try {
                if (INTERVAL_PATTERN.test(literal)) {
                    // a fully specified datetime interval
                    return Interval.parse(literal);
                } else if (INSTANT_PATTERN.test(literal)) {
                    // a fully specified datetime instant
                    return Instant.parse(literal);
                } else if (DATE_PATTERN.test(literal)) {
                    // a date only instant
                    return LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(literal))
                                    .atStartOfDay()
                                    .toInstant(ZoneOffset.UTC);
                } else if (INTERVAL_OPEN_PATTERN.test(literal)) {
                    // open start and end
                    return Interval.of(MIN_DATE, MAX_DATE);
                } else if (INTERVAL_OPEN_END_PATTERN.test(literal)) {
                    // start datetime instant, end open
                    Instant start = Instant.parse(literal.substring(0, literal.indexOf("/")));
                    return Interval.of(start, MAX_DATE);
                } else if (INTERVAL_OPEN_START_PATTERN.test(literal)) {
                    // start open, end datetime instant
                    Instant end = Instant.parse(literal.substring(literal.indexOf("/") + 1));
                    return Interval.of(MIN_DATE, end);
                } else if (DATE_INTERVAL_PATTERN.test(literal)) {
                    // start date instant, end date instant
                    Instant start = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(literal.substring(0, literal.indexOf("/"))))
                                             .atStartOfDay()
                                             .toInstant(ZoneOffset.UTC);
                    Instant end = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(literal.substring(literal.indexOf("/") + 1)))
                                           .atStartOfDay()
                                           .toInstant(ZoneOffset.UTC);
                    return Interval.of(start, end);
                } else if (DATE_INTERVAL_OPEN_END_PATTERN.test(literal)) {
                    // start date instant, end open
                    Instant start = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(literal.substring(0, literal.indexOf("/"))))
                                             .atStartOfDay()
                                             .toInstant(ZoneOffset.UTC);
                    return Interval.of(start, MAX_DATE);
                } else if (DATE_INTERVAL_OPEN_START_PATTERN.test(literal)) {
                    // start open, end date instant
                    Instant end = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(literal.substring(literal.indexOf("/") + 1)))
                                           .atStartOfDay()
                                           .toInstant(ZoneOffset.UTC);
                    return Interval.of(MIN_DATE, end);
                }
            } catch (DateTimeParseException e) {
                //ignore
            }

            throw new CqlParseException("not a valid temporal literal: " + literal);
        }
    }


}
