package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.app.ImmutableCoordinatesWriterFeatureTokens;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.geometries.domain.CoordinatesTransformer;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class FeatureTokenTransformerValueMappings extends FeatureTokenTransformer {

  private final Map<String, List<FeaturePropertyValueTransformer>> propertyValueTransformers;
  private final Optional<CrsTransformer> crsTransformer;
  private ImmutableCoordinatesTransformer.Builder coordinatesTransformerBuilder;
  private int targetDimension;

  public FeatureTokenTransformerValueMappings(
      Map<String, List<FeaturePropertyValueTransformer>> propertyValueTransformers,
      Optional<CrsTransformer> crsTransformer) {
    this.propertyValueTransformers = propertyValueTransformers;
    this.crsTransformer = crsTransformer;
  }

  @Override
  public void onObjectStart(ModifiableContext context) {
    if (context.currentSchema()
    .filter(SchemaBase::isGeometry)
    .isPresent()
        || context.geometryType().isPresent()) {
      this.coordinatesTransformerBuilder = ImmutableCoordinatesTransformer.builder();

      if (crsTransformer.isPresent()) {
        coordinatesTransformerBuilder.crsTransformer(crsTransformer.get());
      }

      int fallbackDimension = context.geometryDimension().orElse(2);
      int sourceDimension = crsTransformer.map(CrsTransformer::getSourceDimension).orElse(fallbackDimension);
      this.targetDimension = crsTransformer.map(CrsTransformer::getTargetDimension).orElse(fallbackDimension);
      coordinatesTransformerBuilder.sourceDimension(sourceDimension);
      coordinatesTransformerBuilder.targetDimension(targetDimension);

      if (context.query().getMaxAllowableOffset() > 0) {
        int minPoints = context.geometryType().get() == SimpleFeatureGeometry.MULTI_POLYGON
            || context.geometryType().get() == SimpleFeatureGeometry.POLYGON
            ? 4
            : 2;
        coordinatesTransformerBuilder.maxAllowableOffset(context.query().getMaxAllowableOffset());
        coordinatesTransformerBuilder.minNumberOfCoordinates(minPoints);
      }

      //TODO: currently never true, see GeotoolsCrsTransformer.needsAxisSwap, find cfg example with forceAxisOrder
    /*if (transformationContext.shouldSwapCoordinates()) {
      coordinatesTransformerBuilder.isSwapXY(true);
    }*/

      if (context.query().getGeometryPrecision() > 0) {
        coordinatesTransformerBuilder.precision(context.query().getGeometryPrecision());
      }

      //TODO: currently never true, see FeatureProperty.isForceReversePolygon, find cfg example
    /*if (Objects.equals(featureProperty.isForceReversePolygon(), true)) {
      coordinatesTransformerBuilder.isReverseOrder(true);
    }*/
    }

    getDownstream().onObjectStart(context);
  }

  @Override
  public void onValue(ModifiableContext context) {
    if (context.inGeometry()) {
      CoordinatesTransformer coordinatesTransformer = coordinatesTransformerBuilder.coordinatesWriter(
          ImmutableCoordinatesWriterFeatureTokens.of(getDownstream(), targetDimension, context))
          .build();
      try {
        coordinatesTransformer.write(context.value());
        coordinatesTransformer.close();
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    } else {
      String value = context.value();
      for (FeaturePropertyValueTransformer valueTransformer : propertyValueTransformers.getOrDefault(context.pathTracker().toString(),
          ImmutableList.of())) {
        value = valueTransformer.transform(value);
        if (Objects.isNull(value))
          break;
      }

      // skip, if the value has been transformed to null
      if (Objects.nonNull(value)) {
        context.setValue(value);
        getDownstream().onValue(context);
      }
    }
  }
}
