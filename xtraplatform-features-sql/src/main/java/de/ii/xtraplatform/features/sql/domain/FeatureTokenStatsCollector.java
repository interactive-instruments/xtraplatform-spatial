/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.ImmutableMutationResult.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureTokenStatsCollector extends FeatureTokenTransformerSql {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTokenStatsCollector.class);

  private final Builder builder;
  private int axis = 0;
  private int dim = 2;
  private EpsgCrs crs = OgcCrs.CRS84;
  private String xmin = "";
  private String ymin = "";
  private String xmax = "";
  private String ymax = "";

  public FeatureTokenStatsCollector(Builder builder) {
    this.builder = builder;
  }

  @Override
  public void onStart(ModifiableContext<SchemaSql, SchemaMappingSql> context) {
    // TODO: get crs
    this.dim = context.geometryDimension().orElse(2);

    super.onStart(context);
  }

  @Override
  public void onEnd(ModifiableContext<SchemaSql, SchemaMappingSql> context) {
    builder.spatialExtent(
        BoundingBox.of(
            Double.parseDouble(xmin),
            Double.parseDouble(ymin),
            Double.parseDouble(xmax),
            Double.parseDouble(ymax),
            crs));

    super.onEnd(context);
  }

  @Override
  public void onValue(ModifiableContext<SchemaSql, SchemaMappingSql> context) {

    /*if (context.schema().filter(SchemaBase::isPrimaryInstant).isPresent()) {
      // TODO: enable date transformation
      LOGGER.debug("INSTANT {} {}", context.pathAsString(), context.value());
    }
    if (context.schema().filter(SchemaBase::isPrimaryIntervalStart).isPresent()) {
      LOGGER.debug("START {} {}", context.pathAsString(), context.value());
    }
    if (context.schema().filter(SchemaBase::isPrimaryIntervalEnd).isPresent()) {
      LOGGER.debug("END {} {}", context.pathAsString(), context.value());
    }*/
    if (context.inGeometry()) {

      if (axis == 0 && (xmin.isEmpty() || context.value().compareTo(xmin) < 0)) {
        this.xmin = context.value();
      }
      if (axis == 0 && (xmax.isEmpty() || context.value().compareTo(xmax) > 0)) {
        this.xmax = context.value();
      }
      if (axis == 1 && (ymin.isEmpty() || context.value().compareTo(ymin) < 0)) {
        this.ymin = context.value();
      }
      if (axis == 1 && (ymax.isEmpty() || context.value().compareTo(ymax) > 0)) {
        this.ymax = context.value();
      }

      this.axis = (axis + 1) % dim;
    }

    super.onValue(context);
  }
}
