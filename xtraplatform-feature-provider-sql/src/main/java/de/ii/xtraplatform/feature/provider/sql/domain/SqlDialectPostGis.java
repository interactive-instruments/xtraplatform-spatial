/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.base.Splitter;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SqlDialectPostGis implements SqlDialect {

    private final static Splitter BBOX_SPLITTER = Splitter.onPattern("[(), ]")
                                                          .omitEmptyStrings()
                                                          .trimResults();

    @Override
    public String applyToWkt(String column) {
        return String.format("ST_AsText(ST_ForcePolygonCCW(%s))", column);
    }

    @Override
    public String applyToExtent(String column) {
        return String.format("ST_Extent(%s)", column);
    }

    @Override
    public Optional<BoundingBox> parseExtent(String extent, EpsgCrs crs) {
        List<String> bbox = BBOX_SPLITTER.splitToList(extent);

        if (bbox.size() > 4) {
            return Optional.of(new BoundingBox(Double.parseDouble(bbox.get(1)), Double.parseDouble(bbox.get(2)), Double.parseDouble(bbox.get(3)), Double.parseDouble(bbox.get(4)), crs));
        }

        return Optional.empty();
    }

    @Override
    public Optional<Interval> parseTemporalExtent(String start, String end) {
        if (Objects.isNull(start)) {
            return Optional.empty();
        }
        DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss][.SSS][X]").withZone(ZoneOffset.UTC);
        Instant parsedStart = parser.parse(start, Instant::from);
        if (Objects.isNull(end)) {
            return Optional.of(Interval.of(parsedStart, Instant.MAX));
        }
        Instant parsedEnd = parser.parse(end, Instant::from);
        return Optional.of(Interval.of(parsedStart, parsedEnd));
    }

    @Override
    public String applyToDatetime(String column) {
        return String.format("%s::timestamp(0)", column);
    }

}
