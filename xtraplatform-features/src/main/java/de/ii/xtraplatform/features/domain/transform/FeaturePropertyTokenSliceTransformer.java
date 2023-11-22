/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.base.Joiner;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenType;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;

public interface FeaturePropertyTokenSliceTransformer
    extends FeaturePropertyTransformer<List<Object>> {

  FeatureSchema transformSchema(FeatureSchema schema);

  void transformObject(
      String currentPropertyPath,
      List<Object> slice,
      List<String> rootPath,
      int start,
      int end,
      List<Object> result);

  @Override
  default List<Object> transform(String currentPropertyPath, List<Object> slice) {
    if (slice.isEmpty()) {
      return slice;
    }

    List<String> rootPath = getRootPath(slice);
    boolean isArray = slice.get(0) == FeatureTokenType.ARRAY;
    List<Object> transformed = new ArrayList<>();

    if (!isArray) {
      transformObject(currentPropertyPath, slice, rootPath, 0, slice.size(), transformed);

      return transformed;
    }

    transformed.add(FeatureTokenType.ARRAY);
    transformed.add(rootPath);

    int start = findPos(slice, FeatureTokenType.OBJECT, rootPath, 0);
    int end = findPos(slice, FeatureTokenType.OBJECT_END, rootPath, start);

    while (start > -1 && end > -1) {
      transformObject(currentPropertyPath, slice, rootPath, start, end + 2, transformed);

      start = findPos(slice, FeatureTokenType.OBJECT, rootPath, end);
      end = findPos(slice, FeatureTokenType.OBJECT_END, rootPath, start);
    }

    transformed.add(FeatureTokenType.ARRAY_END);
    transformed.add(rootPath);

    return transformed;
  }

  Joiner PATH_JOINER = Joiner.on('.');

  default String pathAsString(List<String> path) {
    return PATH_JOINER.join(path);
  }

  default List<String> getRootPath(List<Object> slice) {
    if (slice.size() < 2
        || (slice.get(0) != FeatureTokenType.OBJECT && slice.get(0) != FeatureTokenType.ARRAY)
        || !(slice.get(1) instanceof List)) {
      throw new IllegalArgumentException("Not a valid object or array");
    }

    return (List<String>) slice.get(1);
  }

  default void checkObject(FeatureSchema schema) {
    if (schema.getType() != Type.OBJECT && schema.getType() != Type.OBJECT_ARRAY) {
      throw new IllegalArgumentException(
          String.format(
              "Transformer %s can only be applied to OBJECT or OBJECT_ARRAY, found: %s",
              getType(), schema.getType()));
    }
  }

  default void checkValue(FeatureSchema schema) {
    if (!schema.isValue() || schema.isArray()) {
      throw new IllegalArgumentException(
          String.format(
              "Transformer %s can only be applied to VALUE, found: %s",
              getType(), schema.getType()));
    }
  }

  default void checkArray(FeatureSchema schema) {
    if (schema.getType() != Type.VALUE_ARRAY && schema.getType() != Type.OBJECT_ARRAY) {
      throw new IllegalArgumentException(
          String.format(
              "Transformer %s can only be applied to VALUE_ARRAY or OBJECT_ARRAY, found: %s",
              getType(), schema.getType()));
    }
  }

  default int findPos(List<Object> slice, FeatureTokenType type, List<String> path, int offset) {
    if (offset == -1) {
      return -1;
    }

    for (int i = offset; i < slice.size() - 1; i++) {
      if (Objects.equals(slice.get(i), type) && Objects.equals(slice.get(i + 1), path)) {
        return i;
      }
    }

    return -1;
  }

  default List<Object> valueSlice(List<String> path, @Nullable String value, Type type) {
    List<Object> slice = new ArrayList<>();
    slice.add(FeatureTokenType.VALUE);
    slice.add(path);
    slice.add(value);
    slice.add(type);

    return slice;
  }

  default Function<String, String> getValueLookup(
      String currentPropertyPath, Function<String, String> genericLookup, List<Object> slice) {
    return getValueLookup(currentPropertyPath, genericLookup, slice, 0, slice.size());
  }

  default Function<String, String> getValueLookup(
      String currentPropertyPath,
      Function<String, String> genericLookup,
      List<Object> slice,
      int from,
      int to) {
    Map<String, Integer> valueIndexes = getValueIndexes(slice, from, to);

    return key -> {
      if (Objects.isNull(key)) {
        return null;
      }

      String lookupWithKey = genericLookup.apply(key);

      if (Objects.nonNull(lookupWithKey)) {
        return lookupWithKey;
      }

      String fullKey = currentPropertyPath + "." + key;
      String lookupWithFullKey = genericLookup.apply(fullKey);

      if (Objects.nonNull(lookupWithFullKey)) {
        return lookupWithFullKey;
      }

      if (valueIndexes.containsKey(key)) {
        return getValue(slice, valueIndexes.get(key));
      }

      if (valueIndexes.containsKey(fullKey)) {
        return getValue(slice, valueIndexes.get(fullKey));
      }

      return null;
    };
  }

  default Map<String, Integer> getValueIndexes(List<Object> slice, int from, int to) {
    Map<String, Integer> valueIndexes = new HashMap<>();

    for (int i = from; i < to; i++) {
      if (slice.get(i) == FeatureTokenType.VALUE) {
        if (i + 2 < to && slice.get(i + 1) instanceof List) {
          valueIndexes.put(joinPath((List<String>) slice.get(i + 1)), i + 2);
        }
      }
    }

    return valueIndexes;
  }

  default boolean isValueWithPath(List<Object> slice, int index, List<String> path) {
    return slice.get(index) == FeatureTokenType.VALUE
        && index + 3 < slice.size()
        && slice.get(index + 1) instanceof List
        && Objects.equals(slice.get(index + 1), path);
  }

  default boolean isObjectWithPath(List<Object> slice, int index, List<String> path) {
    return slice.get(index) == FeatureTokenType.OBJECT
        && index + 3 < slice.size()
        && slice.get(index + 1) instanceof List
        && Objects.equals(slice.get(index + 1), path);
  }

  default boolean isObjectEndWithPath(List<Object> slice, int index, List<String> path) {
    return slice.get(index) == FeatureTokenType.OBJECT_END
        && index + 3 < slice.size()
        && slice.get(index + 1) instanceof List
        && Objects.equals(slice.get(index + 1), path);
  }

  default boolean isChildOfPath(List<Object> slice, int index, List<String> path) {
    return slice.get(index) instanceof FeatureTokenType
        && index + 1 < slice.size()
        && slice.get(index + 1) instanceof List
        && ((List<?>) slice.get(index + 1)).size() > path.size()
        && Objects.equals(path, ((List<String>) slice.get(index + 1)).subList(0, path.size()));
  }

  static String joinPath(List<String> path) {
    String last = path.get(path.size() - 1);
    if (last.matches("[0-9]+_.*")) {
      List<String> path2 = new ArrayList<>(path.subList(0, path.size() - 1));
      path2.add(last.substring(last.indexOf("_") + 1));

      return PATH_JOINER.join(path2);
    }

    return PATH_JOINER.join(path);
  }

  static String getValue(List<Object> slice, int valueIndex) {
    if (valueIndex < slice.size() && slice.get(valueIndex) instanceof String) {
      return (String) slice.get(valueIndex);
    }

    return null;
  }

  static int getValueIndex(List<Object> slice, String path, int from, int to) {
    for (int i = from; i < to; i++) {
      if (slice.get(i) == FeatureTokenType.VALUE) {
        if (i + 2 < to && slice.get(i + 1) instanceof List) {
          if (Objects.equals(path, PATH_JOINER.join((List<String>) slice.get(i + 1)))) {
            return i + 2;
          }
        }
      }
    }

    return -1;
  }

  default String getValue(List<Object> slice, String path) {
    return getValue(slice, path, 0, slice.size());
  }

  default String getValue(List<Object> slice, String path, int start, int end) {
    int valueIndex = getValueIndex(slice, path, start, end);

    if (valueIndex < 0) {
      return null;
    }

    return (String) slice.get(valueIndex);
  }

  default Tuple<String, Type> getValueAndType(List<Object> slice, String path) {
    return getValueAndType(slice, path, 0, slice.size());
  }

  default Tuple<String, Type> getValueAndType(List<Object> slice, String path, int start, int end) {
    int valueIndex = getValueIndex(slice, path, start, end);

    if (valueIndex < 0) {
      return Tuple.of(null, Type.STRING);
    }

    String value = (String) slice.get(valueIndex);
    Type type =
        slice.size() > valueIndex + 1 && slice.get(valueIndex + 1) instanceof Type
            ? (Type) slice.get(valueIndex + 1)
            : Type.STRING;

    return Tuple.of(value, type);
  }
}
