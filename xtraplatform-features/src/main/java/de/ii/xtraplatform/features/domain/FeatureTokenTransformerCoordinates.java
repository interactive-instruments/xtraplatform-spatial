/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.app.ImmutableCoordinatesWriterFeatureTokens;
import de.ii.xtraplatform.geometries.domain.CoordinatesTransformer;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.util.Optional;

public class FeatureTokenTransformerCoordinates extends FeatureTokenTransformer {

  private final Optional<CrsTransformer> crsTransformer;
  private ImmutableCoordinatesTransformer.Builder coordinatesTransformerBuilder;
  private int targetDimension;

  public FeatureTokenTransformerCoordinates(Optional<CrsTransformer> crsTransformer) {
    this.crsTransformer = crsTransformer;
  }

  @Override
  public void onObjectStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (context.schema().filter(SchemaBase::isSpatial).isPresent()
        || context.geometryType().isPresent()) {
      this.coordinatesTransformerBuilder = ImmutableCoordinatesTransformer.builder();

      if (crsTransformer.isPresent()) {
        coordinatesTransformerBuilder.crsTransformer(crsTransformer.get());
      }

      int fallbackDimension = context.geometryDimension().orElse(2);
      int sourceDimension =
          crsTransformer.map(CrsTransformer::getSourceDimension).orElse(fallbackDimension);
      this.targetDimension =
          crsTransformer.map(CrsTransformer::getTargetDimension).orElse(fallbackDimension);
      coordinatesTransformerBuilder.sourceDimension(sourceDimension);
      coordinatesTransformerBuilder.targetDimension(targetDimension);

      if (context.query().getMaxAllowableOffset() > 0) {
        int minPoints =
            context.geometryType().get() == SimpleFeatureGeometry.MULTI_POLYGON
                    || context.geometryType().get() == SimpleFeatureGeometry.POLYGON
                ? 4
                : 2;
        coordinatesTransformerBuilder.maxAllowableOffset(context.query().getMaxAllowableOffset());
        coordinatesTransformerBuilder.minNumberOfCoordinates(minPoints);
      }

      // TODO: currently never true, see GeotoolsCrsTransformer.needsAxisSwap, find cfg example with
      // forceAxisOrder
      /*if (transformationContext.shouldSwapCoordinates()) {
        coordinatesTransformerBuilder.isSwapXY(true);
      }*/

      if (context.query().getGeometryPrecision().get(0) > 0) {
        coordinatesTransformerBuilder.precision(context.query().getGeometryPrecision());
      }

      // TODO: currently never true, see FeatureProperty.isForceReversePolygon, find cfg example
      /*if (Objects.equals(featureProperty.isForceReversePolygon(), true)) {
        coordinatesTransformerBuilder.isReverseOrder(true);
      }*/
    }

    getDownstream().onObjectStart(context);
  }

  @Override
  public void onValue(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (context.inGeometry()) {
      CoordinatesTransformer coordinatesTransformer =
          coordinatesTransformerBuilder
              .coordinatesWriter(
                  ImmutableCoordinatesWriterFeatureTokens.of(
                      getDownstream(), targetDimension, context))
              .build();
      try {
        coordinatesTransformer.write(context.value());
        coordinatesTransformer.close();
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    } else {
      getDownstream().onValue(context);
    }
  }
}
