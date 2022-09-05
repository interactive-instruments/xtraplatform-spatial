/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.app.ImmutableCoordinatesWriterFeatureTokens;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.TransformerChain;
import de.ii.xtraplatform.geometries.domain.CoordinatesTransformer;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.time.ZoneId;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

public class FeatureTokenTransformerValueMappings extends FeatureTokenTransformer {

  private final Map<String, PropertyTransformations> propertyTransformations;
  private final Map<String, Codelist> codelists;
  private final Optional<ZoneId> nativeTimeZone;
  private final Optional<CrsTransformer> crsTransformer;
  private Map<String, TransformerChain<String, FeaturePropertyValueTransformer>>
      valueTransformerChains;
  private TransformerChain<String, FeaturePropertyValueTransformer> valueTransformerChain;
  private ImmutableCoordinatesTransformer.Builder coordinatesTransformerBuilder;
  private int targetDimension;

  public FeatureTokenTransformerValueMappings(
      Map<String, PropertyTransformations> propertyTransformations,
      Map<String, Codelist> codelists,
      Optional<ZoneId> nativeTimeZone,
      Optional<CrsTransformer> crsTransformer) {
    this.propertyTransformations = propertyTransformations;
    this.codelists = codelists;
    this.nativeTimeZone = nativeTimeZone;
    this.crsTransformer = crsTransformer;
  }

  @Override
  public void onStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    this.valueTransformerChains =
        context.mappings().entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        propertyTransformations
                            .get(entry.getKey())
                            .getValueTransformations(
                                entry.getValue(),
                                codelists,
                                nativeTimeZone,
                                context.valueBuffer()::get)))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    super.onStart(context);
  }

  @Override
  public void onFeatureStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    this.valueTransformerChain = valueTransformerChains.get(context.type());

    super.onFeatureStart(context);
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
      String path = context.pathAsString();
      String value = context.value();

      if (!context.valueBuffer().isEmpty()) {
        transformValueBuffer(context, path);
      }
      value = valueTransformerChain.transform(path, value);

      // skip, if the value has been transformed to null
      if (Objects.nonNull(value)) {
        context.setValue(value);
        getDownstream().onValue(context);
      }
    }
  }

  private void transformValueBuffer(
      ModifiableContext<FeatureSchema, SchemaMapping> context, String path) {
    for (Iterator<Entry<String, String>> it = context.valueBuffer().entrySet().iterator();
        it.hasNext(); ) {
      Map.Entry<String, String> entry = it.next();
      String key = entry.getKey();

      if (key.startsWith(path + ".")) {
        String transformed = valueTransformerChain.transform(key, entry.getValue());
        if (Objects.nonNull(transformed)) {
          context.putValueBuffer(key, transformed);
        } else {
          it.remove();
        }
      }
    }
  }
}
