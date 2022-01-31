/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.util.Optional;

@JsonSubTypes({
        @JsonSubTypes.Type(value = CqlDateTime.CqlDate.class, name = "date"),
        @JsonSubTypes.Type(value = CqlDateTime.CqlTimestamp.class, name = "timestamp"),
        @JsonSubTypes.Type(value = CqlDateTime.CqlInterval.class, name = "interval"),
})
public interface CqlDateTime extends CqlNode {

    enum Type {Date, Timestamp, Interval}

    CqlDateTime.Type getType();

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableCqlDate.Builder.class)
    interface CqlDate extends CqlDateTime {

        static CqlDate of(Interval date) {
            return new ImmutableCqlDate.Builder().date(date).build();
        }

        Interval getDate();

        @Override
        default CqlDateTime.Type getType() {
            return Type.Date;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableCqlTimestamp.Builder.class)
    interface CqlTimestamp extends CqlDateTime {

        static CqlTimestamp of(Instant timestamp) {
            return new ImmutableCqlTimestamp.Builder().timestamp(timestamp).build();
        }

        Instant getTimestamp();

        @Override
        default CqlDateTime.Type getType() {
            return Type.Timestamp;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableCqlInterval.Builder.class)
    interface CqlInterval extends CqlDateTime {

        static CqlInterval of(Interval interval) {
            return new ImmutableCqlInterval.Builder().interval(interval).build();
        }

        static CqlInterval of(Temporal start, Temporal end) {
            return new ImmutableCqlInterval.Builder().start(start).end(end).build();
        }

        Optional<Interval> getInterval();

        Optional<Temporal> getStart();

        Optional<Temporal> getEnd();

        @Override
        default CqlDateTime.Type getType() {
            return Type.Interval;
        }

    }

}
