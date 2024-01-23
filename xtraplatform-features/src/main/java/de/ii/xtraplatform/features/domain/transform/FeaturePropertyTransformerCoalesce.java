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
import java.util.Objects;
import org.immutables.value.Value;

@Value.Immutable
public abstract class FeaturePropertyTransformerCoalesce
    implements FeaturePropertyTokenSliceTransformer {

  private static final String TYPE = "COALESCE";

  private FeatureSchema schema;

  @Override
  public String getType() {
    return TYPE;
  }

  public abstract boolean isObject();

  @Override
  public FeatureSchema transformSchema(FeatureSchema schema) {
    // checkArray(schema);
    this.schema = schema;

    return schema.with(builder -> builder.type(isObject() ? Type.OBJECT : Type.VALUE));
  }

  @Override
  public List<Object> transform(String currentPropertyPath, List<Object> slice) {
    if (slice.isEmpty() || Objects.isNull(schema)) {
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
      /*int start = findPos(slice, FeatureTokenType.VALUE, rootPath, 0);

      if (start > -1) {
        transformed.addAll(slice.subList(start, start + 4));
      }*/
      return coalesceValues(slice);
    }

    return transformed;
  }

  private List<Object> coalesceValues(List<Object> slice) {
    List<Object> transformed = new ArrayList<>();
    boolean skip = false;
    boolean found = false;

    for (int i = 0; i < slice.size(); i++) {
      if (isValueWithPath(slice, i, schema.getFullPath())) {
        if (found) {
          break;
        }
        if (isNonNullValue(slice, i)) {
          skip = false;
          found = true;
        } else {
          skip = true;
        }
      } else if (slice.get(i) instanceof FeatureTokenType) {
        if (found) {
          break;
        }
        skip = true;
      }
      if (!skip) {
        transformed.add(slice.get(i));
      }
    }

    return transformed;
  }

  @Override
  public void transformObject(
      String currentPropertyPath,
      List<Object> slice,
      List<String> rootPath,
      int start,
      int end,
      List<Object> result) {}
}
