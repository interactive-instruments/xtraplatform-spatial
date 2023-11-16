/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenType;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.util.ArrayList;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerCoalesce extends FeaturePropertyTokenSliceTransformer {

  String TYPE = "COALESCE";

  @Override
  default String getType() {
    return TYPE;
  }

  boolean isObject();

  @Override
  default FeatureSchema transformSchema(FeatureSchema schema) {
    // checkArray(schema);

    return schema.with(builder -> builder.type(isObject() ? Type.OBJECT : Type.VALUE));
  }

  @Override
  default List<Object> transform(String currentPropertyPath, List<Object> slice) {
    if (slice.isEmpty()) {
      return slice;
    }

    List<String> rootPath = getRootPath(slice);
    boolean isArray = slice.get(0) == FeatureTokenType.ARRAY;
    List<Object> transformed = new ArrayList<>();

    if (!isArray) {
      return slice;
    }

    if (isObject()) {
      int start = findPos(slice, FeatureTokenType.OBJECT, rootPath, 0);

      if (start > -1) {
        int end = findPos(slice, FeatureTokenType.OBJECT_END, rootPath, start);

        transformed.addAll(slice.subList(start, end + 2));
      }
    } else {
      int start = findPos(slice, FeatureTokenType.VALUE, rootPath, 0);

      if (start > -1) {
        transformed.addAll(slice.subList(start, start + 4));
      }
    }

    return transformed;
  }

  @Override
  default void transformObject(
      String currentPropertyPath,
      List<Object> slice,
      List<String> rootPath,
      int start,
      int end,
      List<Object> result) {}
}
