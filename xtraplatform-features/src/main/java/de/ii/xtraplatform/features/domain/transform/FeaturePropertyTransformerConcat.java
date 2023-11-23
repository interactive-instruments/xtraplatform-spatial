/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import static de.ii.xtraplatform.features.domain.FeatureTokenType.ARRAY;
import static de.ii.xtraplatform.features.domain.FeatureTokenType.ARRAY_END;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenType;
import java.util.ArrayList;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public abstract class FeaturePropertyTransformerConcat
    implements FeaturePropertyTokenSliceTransformer {

  private static final String TYPE = "CONCAT";

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

    return schema;
  }

  @Override
  public List<Object> transform(String currentPropertyPath, List<Object> slice) {
    if (slice.isEmpty()) {
      return slice;
    }

    boolean isArray = slice.get(0) == ARRAY;
    List<Object> transformed = new ArrayList<>();

    if (!isArray) {
      return slice;
    }

    if (isObject()) {
      return concatObjects(slice);
    } else {
      // return coalesceValues(slice);
    }

    return transformed;
  }

  private List<Object> concatObjects(List<Object> slice) {
    List<Object> transformed = new ArrayList<>();
    boolean isArrayOpen = false;
    boolean skip = false;

    for (int i = 0; i < slice.size(); i++) {
      if (isTypeWithPath(slice, i, ARRAY, schema.getFullPath())) {
        if (!isArrayOpen) {
          isArrayOpen = true;
        } else {
          skip = true;
        }
      } else if (isTypeWithPath(slice, i, ARRAY_END, schema.getFullPath())) {
        if (i < slice.size() - 2) {
          skip = true;
        } else {
          isArrayOpen = false;
        }
      } else if (slice.get(i) instanceof FeatureTokenType) {
        skip = false;
      }
      if (!skip) {
        transformed.add(slice.get(i));
      }
    }

    if (isArrayOpen) {
      transformed.add(ARRAY_END);
      transformed.add(schema.getFullPath());
    }

    return transformed;
  }

  private List<Object> coalesceValues(List<Object> slice) {
    List<Object> transformed = new ArrayList<>();
    boolean lastWasValueWithPath = false;
    boolean skip = false;

    for (int i = 0; i < slice.size(); i++) {
      if (isValueWithPath(slice, i, schema.getFullPath())) {
        if (!lastWasValueWithPath) {
          lastWasValueWithPath = true;
        } else {
          skip = true;
        }
      } else if (slice.get(i) instanceof FeatureTokenType) {
        lastWasValueWithPath = false;
        skip = false;
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
