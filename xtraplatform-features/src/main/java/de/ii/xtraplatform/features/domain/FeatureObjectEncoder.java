/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.PropertyBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class FeatureObjectEncoder<T extends PropertyBase<T, FeatureSchema>, U extends FeatureBase<T, FeatureSchema>> extends
    FeatureTokenEncoder<ModifiableContext> {

  private U currentFeature;
  private T currentObjectOrArray;

  public abstract U createFeature();

  public abstract T createProperty();

  public abstract void onFeature(U feature);

  @Override
  public void onStart(ModifiableContext context) {

  }

  @Override
  public void onEnd(ModifiableContext context) {

  }

  @Override
  public final void onFeatureStart(ModifiableContext context) {
    if (context.schema().isEmpty()) {
      return;
    }

    this.currentFeature = createFeature();
    currentFeature.schema(context.schema().get());

    currentFeature.collectionMetadata(context.metadata());

    this.currentObjectOrArray = null;
  }

  @Override
  public final void onFeatureEnd(ModifiableContext context) {
    onFeature(currentFeature);

    this.currentFeature = null;
    this.currentObjectOrArray = null;
  }

  @Override
  public final void onObjectStart(ModifiableContext context) {
    if (context.schema().isEmpty()) {
      return;
    }

    this.currentObjectOrArray = createProperty(PropertyBase.Type.OBJECT, context.path(), context.schema().get(), context.geometryType().orElse(null));
  }

  @Override
  public final void onObjectEnd(ModifiableContext context) {
    this.currentObjectOrArray = getCurrentParent();
  }

  @Override
  public final void onArrayStart(ModifiableContext context) {
    if (context.schema().isEmpty()) {
      return;
    }

    this.currentObjectOrArray = createProperty(PropertyBase.Type.ARRAY, context.path(), context.schema().get());
  }

  @Override
  public final void onArrayEnd(ModifiableContext context) {
    this.currentObjectOrArray = getCurrentParent();
  }

  @Override
  public final void onValue(ModifiableContext context) {
    if (context.schema().isEmpty() || Objects.isNull(context.value())) {
      return;
    }

    createProperty(PropertyBase.Type.VALUE, context.path(), context.schema().get(), context.value(), context.transformed());
  }

  @Override
  public final Class<? extends ModifiableContext> getContextInterface() {
    return ModifiableContext.class;
  }

  @Override
  public final ModifiableContext createContext() {
    return ModifiableGenericContext.create();
  }

  private T createProperty(Property.Type type, List<String> path, FeatureSchema schema) {
    return createProperty(type, path, schema, null, null, ImmutableMap.of());
  }

  private T createProperty(Property.Type type, List<String> path, FeatureSchema schema, SimpleFeatureGeometry geometryType) {
    return createProperty(type, path, schema, null, geometryType, ImmutableMap.of());
  }

  private T createProperty(Type type, List<String> path, FeatureSchema schema,
                           String value, Map<String, String> transformed) {
    return createProperty(type, path, schema, value, null, transformed);
  }

  private T createProperty(Type type, List<String> path, FeatureSchema schema,
      String value, SimpleFeatureGeometry geometryType, Map<String, String> transformed) {

    /*return currentFeature.getProperties()
        .stream()
        .filter(t -> t.getType() == type && t.getSchema().isPresent() && Objects
            .equals(t.getSchema().get(), schema) && !t.getSchema().get().isGeometry())
        .findFirst()
        .orElseGet(() -> {*/
          T property = createProperty();
          property.type(type)
                  .schema(schema)
                  .propertyPath(path)
                  .value(value)
                  .geometryType(Optional.ofNullable(geometryType))
                  .transformed(transformed);

          if (Objects.nonNull(currentObjectOrArray)) {
            property.parent(currentObjectOrArray);
            currentObjectOrArray.addNestedProperties(property);
          } else {
            currentFeature.addProperties(property);
          }

          return property;
        //});
  }

  private T getCurrentParent() {
    return Objects.nonNull(currentObjectOrArray) && currentObjectOrArray.getParent()
        .isPresent()
        ? currentObjectOrArray.getParent()
        .get()
        : null;
  }
}
