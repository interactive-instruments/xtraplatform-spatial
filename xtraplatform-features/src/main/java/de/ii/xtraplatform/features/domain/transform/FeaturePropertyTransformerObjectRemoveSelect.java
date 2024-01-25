/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.List;
import java.util.Objects;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerObjectRemoveSelect
    extends FeaturePropertyTokenSliceTransformer {

  String TYPE = "OBJECT_NULLIFY_SELECT";

  @Override
  default String getType() {
    return TYPE;
  }

  @Override
  default FeatureSchema transformSchema(FeatureSchema schema) {
    checkObject(schema);

    if (findProperty(schema, getParameter()).isEmpty()) {
      throw new IllegalArgumentException("Selected property not found: " + getParameter());
    }

    return schema;
  }

  @Override
  default void transformObject(
      String currentPropertyPath,
      List<Object> slice,
      List<String> rootPath,
      int start,
      int end,
      List<Object> result) {
    String value = getValue(slice, getPropertyPath() + "." + getParameter(), start, end);

    if (Objects.nonNull(value)) {
      result.addAll(slice.subList(start, end));
    }
  }
}
