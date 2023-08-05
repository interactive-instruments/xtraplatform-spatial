/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class FeatureTokenTransformerMetadata extends FeatureTokenTransformer {

  private final Consumer<Instant> lastModifiedSetter;
  private final Consumer<BoundingBox> spatialExtentSetter;
  private final Consumer<Tuple<Instant, Instant>> temporalExtentSetter;
  private Optional<EpsgCrs> crs;
  private int axis = 0;
  private int dim = 2;
  private String xmin = "";
  private String ymin = "";
  private String xmax = "";
  private String ymax = "";
  private String start = "";
  private String end = "";
  private boolean isSingleFeature = false;
  private String lastModified = "";

  public FeatureTokenTransformerMetadata(ImmutableResult.Builder resultBuilder) {
    this.lastModifiedSetter = resultBuilder::lastModified;
    this.spatialExtentSetter = resultBuilder::spatialExtent;
    this.temporalExtentSetter = resultBuilder::temporalExtent;
  }

  public <X> FeatureTokenTransformerMetadata(ImmutableResultReduced.Builder<X> resultBuilder) {
    this.lastModifiedSetter = resultBuilder::lastModified;
    this.spatialExtentSetter = resultBuilder::spatialExtent;
    this.temporalExtentSetter = resultBuilder::temporalExtent;
  }

  @Override
  public void onStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    // TODO from CRS, requires crsInfo or add info to query
    this.dim = context.geometryDimension().orElse(2);
    this.crs = context.query().getCrs();
    this.isSingleFeature = context.metadata().isSingleFeature();

    super.onStart(context);
  }

  @Override
  public void onEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    try {
      if (!xmin.isEmpty() && !ymin.isEmpty() && !xmax.isEmpty() && !ymax.isEmpty()) {
        spatialExtentSetter.accept(
            BoundingBox.of(
                Double.parseDouble(xmin),
                Double.parseDouble(ymin),
                Double.parseDouble(xmax),
                Double.parseDouble(ymax),
                crs.orElse(dim == 2 ? OgcCrs.CRS84 : OgcCrs.CRS84h)));
      }
    } catch (Throwable ignore) {}

    try {
      if (!start.isEmpty() && !end.isEmpty()) {
        temporalExtentSetter.accept(Tuple.of(Instant.parse(start), Instant.parse(end)));
      } else if (!start.isEmpty()) {
        temporalExtentSetter.accept(Tuple.of(Instant.parse(start), null));
      } else if (!end.isEmpty()) {
        temporalExtentSetter.accept(Tuple.of(null, Instant.parse(end)));
      }
    } catch (Throwable ignore) {}

    try {
      if (!lastModified.isEmpty()) {
        lastModifiedSetter.accept(Instant.parse(lastModified));
      }
    } catch (Throwable ignore) {}

    super.onEnd(context);
  }

  @Override
  public void onValue(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (Objects.nonNull(context.value())) {
      String value = context.value();

      if (context.inGeometry()) {
        if (axis == 0 && (xmin.isEmpty() || value.compareTo(xmin) < 0)) {
          this.xmin = value;
        }
        if (axis == 0 && (xmax.isEmpty() || value.compareTo(xmax) > 0)) {
          this.xmax = value;
        }
        if (axis == 1 && (ymin.isEmpty() || value.compareTo(ymin) < 0)) {
          this.ymin = value;
        }
        if (axis == 1 && (ymax.isEmpty() || value.compareTo(ymax) > 0)) {
          this.ymax = value;
        }

        this.axis = (axis + 1) % dim;
      } else if (context.schema().filter(SchemaBase::isPrimaryInstant).isPresent()) {
        if (start.isEmpty() || value.compareTo(start) < 0) {
          this.start = value;
        }
        if (end.isEmpty() || value.compareTo(end) > 0) {
          this.end = value;
        }
      } else if (context.schema().filter(SchemaBase::isPrimaryIntervalStart).isPresent()) {
        if (start.isEmpty() || value.compareTo(start) < 0) {
          this.start = value;
        }
      } else if (context.schema().filter(SchemaBase::isPrimaryIntervalEnd).isPresent()) {
        if (end.isEmpty() || value.compareTo(end) > 0) {
          this.end = value;
        }
      }

      if (isSingleFeature && context.schema().map(SchemaBase::lastModified).orElse(false)) {
        this.lastModified = value;
      }
    }

    super.onValue(context);
  }
}
