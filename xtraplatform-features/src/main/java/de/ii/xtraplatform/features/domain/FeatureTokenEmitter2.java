/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public interface FeatureTokenEmitter2<
        T extends SchemaBase<T>, U extends SchemaMappingBase<T>, V extends ModifiableContext<T, U>>
    extends FeatureEventHandler<T, U, V> {

  void push(Object token);

  @Override
  default void onStart(V context) {
    onStart(
        context.metadata().isSingleFeature(),
        context.metadata().getNumberReturned(),
        context.metadata().getNumberMatched());
  }

  default void onStart(
      boolean isSingleFeature, OptionalLong numberReturned, OptionalLong numberMatched) {
    push(FeatureTokenType.INPUT);

    if (isSingleFeature) {
      push(true);
    } else {
      if (numberReturned.isPresent()) {
        push(numberReturned.getAsLong());
      }
      if (numberMatched.isPresent()) {
        push(numberMatched.getAsLong());
      }
    }
  }

  @Override
  default void onEnd(V context) {
    onEnd();
  }

  default void onEnd() {
    push(FeatureTokenType.INPUT_END);
  }

  @Override
  default void onFeatureStart(V context) {
    onFeatureStart(context.path());
  }

  default void onFeatureStart(List<String> path) {
    push(FeatureTokenType.FEATURE);
    if (!path.isEmpty()) {
      push(path);
    }
  }

  @Override
  default void onFeatureEnd(V context) {
    onFeatureEnd();
  }

  default void onFeatureEnd() {
    push(FeatureTokenType.FEATURE_END);
  }

  @Override
  default void onObjectStart(V context) {
    onObjectStart(context.path(), context.geometryType(), context.geometryDimension());
  }

  default void onObjectStart(
      List<String> path,
      Optional<SimpleFeatureGeometry> geometryType,
      OptionalInt geometryDimension) {
    push(FeatureTokenType.OBJECT);
    push(path);
    if (geometryType.isPresent()) {
      push(geometryType.get());
      if (geometryDimension.isPresent()) {
        push(geometryDimension.getAsInt());
      }
    }
  }

  @Override
  default void onObjectEnd(V context) {
    onObjectEnd(context.path());
  }

  default void onObjectEnd(List<String> path) {
    push(FeatureTokenType.OBJECT_END);
    push(path);
  }

  @Override
  default void onArrayStart(V context) {
    onArrayStart(context.path());
  }

  default void onArrayStart(List<String> path) {
    push(FeatureTokenType.ARRAY);
    push(path);
  }

  @Override
  default void onArrayEnd(V context) {
    onArrayEnd(context.path());
  }

  default void onArrayEnd(List<String> path) {
    push(FeatureTokenType.ARRAY_END);
    push(path);
  }

  @Override
  default void onValue(V context) {
    onValue(context.path(), context.value(), context.valueType());
  }

  default void onValue(List<String> path, String value, Type type) {
    push(FeatureTokenType.VALUE);
    push(path);
    if (Objects.nonNull(value)) {
      push(value);
      push(type);
    }
  }
}
