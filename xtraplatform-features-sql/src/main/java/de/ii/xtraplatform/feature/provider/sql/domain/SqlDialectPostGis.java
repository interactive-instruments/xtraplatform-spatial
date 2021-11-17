/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.ImmutableMultiPolygon;
import de.ii.xtraplatform.cql.domain.ImmutablePolygon;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.ii.xtraplatform.crs.domain.OgcCrs;
import org.threeten.extra.Interval;

public class SqlDialectPostGis implements SqlDialect {

  private final static Splitter BBOX_SPLITTER = Splitter.onPattern("[(), ]")
      .omitEmptyStrings()
      .trimResults();

  @Override
  public String applyToWkt(String column, boolean forcePolygonCCW, Optional<Geometry.Envelope> clipbox) {
    if (clipbox.isEmpty()) {
      if (!forcePolygonCCW) {
        return String.format("ST_AsText(%s)", column);
      }
      return String.format("ST_AsText(ST_ForcePolygonCCW(%s))", column);
    }

    List<Double> coords = clipbox.get().getCoordinates();
    int srid = clipbox.get().getCrs().map(EpsgCrs::getCode).orElse(4326);
    // TODO we should get this information from the CRS
    boolean hasDiscontinuityAt180DegreeLongitude = ImmutableList.of(4326, 4979, 4258, 4269).contains(srid);
    String clip;
    if (coords.get(0)>coords.get(2) && hasDiscontinuityAt180DegreeLongitude) {
      // special case, the clip-box crosses the antimeridian, we create convert this to a MultiPolygon
      clip = String.format(Locale.US,"ST_Collect(ST_MakeEnvelope(%f,%f,%f,%f,%d),ST_MakeEnvelope(%f,%f,%f,%f,%d))",
                           coords.get(0), coords.get(1),
                           180.0, coords.get(3),
                           srid,
                           -180.0, coords.get(1),
                           coords.get(2), coords.get(3),
                           srid);
    } else {
      clip = String.format(Locale.US,"ST_MakeEnvelope(%f,%f,%f,%f,%d)",
                           coords.get(0), coords.get(1),
                           coords.get(2), coords.get(3),
                           srid);
    }

    if (!forcePolygonCCW) {
      return String.format("ST_AsText(ST_Intersection(%s,%s))", column, clip);
    }
    return String.format("ST_AsText(ST_Intersection(ST_ForcePolygonCCW(%s),%s))", column, clip);
  }

  @Override
  public String applyToExtent(String column) {
    return String.format("ST_Extent(%s)", column);
  }

  @Override
  public Optional<BoundingBox> parseExtent(String extent, EpsgCrs crs) {
    List<String> bbox = BBOX_SPLITTER.splitToList(extent);

    if (bbox.size() > 4) {
      return Optional.of(BoundingBox
          .of(Double.parseDouble(bbox.get(1)), Double.parseDouble(bbox.get(2)),
              Double.parseDouble(bbox.get(3)), Double.parseDouble(bbox.get(4)), crs));
    }

    return Optional.empty();
  }

  @Override
  public Optional<Interval> parseTemporalExtent(String start, String end) {
    if (Objects.isNull(start)) {
      return Optional.empty();
    }
    DateTimeFormatter parser = DateTimeFormatter
        .ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss][.SSS][X]").withZone(ZoneOffset.UTC);
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

  @Override
  public String escapeString(String value) {
    return value.replaceAll("'", "''");
  }

  @Override
  public String geometryInfoQuery(Map<String, String> dbInfo) {
    return String.format(
        "SELECT f_table_schema AS \"%s\", f_table_name AS \"%s\", f_geometry_column AS \"%s\", coord_dimension AS \"%s\", srid AS \"%s\", type AS \"%s\" FROM geometry_columns;",
        GeoInfo.SCHEMA, GeoInfo.TABLE, GeoInfo.COLUMN, GeoInfo.DIMENSION, GeoInfo.SRID,
        GeoInfo.TYPE);
  }

  @Override
  public List<String> getSystemTables() {
    return ImmutableList
        .of("spatial_ref_sys", "geography_columns", "geometry_columns", "raster_columns",
            "raster_overviews");
  }

}
