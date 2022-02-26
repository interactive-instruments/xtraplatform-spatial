/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.SchemaBase;
import java.time.ZoneId;
import java.util.Optional;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;

@Value.Immutable
public interface FeaturePropertyTransformerDateFormat extends FeaturePropertyValueTransformer {

    Logger LOGGER = LoggerFactory.getLogger(FeaturePropertyTransformerDateFormat.class);

    String TYPE = "DATE_FORMAT";
    ZoneId UTC = ZoneId.of("UTC");
    String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";
    String DATE_FORMAT = "yyyy-MM-dd";

    @Override
    default String getType() {
        return TYPE;
    }

    @Override
    default List<SchemaBase.Type> getSupportedPropertyTypes() {
        return ImmutableList.of(SchemaBase.Type.DATETIME);
    }

    Optional<ZoneId> getDefaultTimeZone();

    @Override
    default String transform(String currentPropertyPath, String input) {
        //TODO: variable fractions
        try {
            DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss][.SSS][X]");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(getParameter());
            TemporalAccessor ta = parser.parseBest(input, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
            if (ta instanceof OffsetDateTime) {
                ta = ((OffsetDateTime)ta).atZoneSameInstant(UTC);
            } else if (ta instanceof LocalDateTime) {
                ta = ((LocalDateTime)ta).atZone(getDefaultTimeZone().orElse(UTC)).withZoneSameInstant(UTC);
            } 

            return formatter.format(ta);
        } catch (Exception e) {
            LOGGER.warn("{} transformation for property '{}' with value '{}' failed: {}", getType(), getPropertyPath(), input, e.getMessage());
        }

        return input;
    }
}
