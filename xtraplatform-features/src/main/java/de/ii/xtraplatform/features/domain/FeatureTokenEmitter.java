/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public interface FeatureTokenEmitter extends FeatureEventConsumer {

  void push(Object token);

  @Override
  default void onStart(OptionalLong numberReturned, OptionalLong numberMatched) {
    push(FeatureTokenType.INPUT);
    if (numberReturned.isPresent()) {
      push(numberReturned.getAsLong());
    }
    if (numberMatched.isPresent()) {
      push(numberMatched.getAsLong());
    }
  }

  @Override
  default void onEnd() {
    push(FeatureTokenType.INPUT_END);
  }

  @Override
  default void onFeatureStart() {
    push(FeatureTokenType.FEATURE);
  }

  @Override
  default void onFeatureEnd() {
    push(FeatureTokenType.FEATURE_END);
  }

  @Override
  default void onObjectStart(List<String> path, Optional<SimpleFeatureGeometry> geometryType) {
    push(FeatureTokenType.OBJECT);
    push(path);
    if (geometryType.isPresent()) {
      push(geometryType);
    }
  }

  @Override
  default void onObjectEnd() {
    push(FeatureTokenType.OBJECT_END);
  }

  @Override
  default void onArrayStart(List<String> path) {
    push(FeatureTokenType.ARRAY);
    push(path);
  }

  @Override
  default void onArrayEnd() {
    push(FeatureTokenType.ARRAY_END);
  }

  @Override
  default void onValue(List<String> path, String value, SchemaBase.Type valueType) {
    push(FeatureTokenType.VALUE);
    push(path);
    if (Objects.nonNull(value)) {
      push(value);
      push(valueType);
    }
  }
}
