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

  Joiner PATH_JOINER = Joiner.on('.');

  default String pathAsString(List<String> path) {
    return PATH_JOINER.join(path);
  }

  default List<String> getRootObjectPath(List<Object> slice) {
    if (slice.size() < 2
        || slice.get(0) != FeatureTokenType.OBJECT
        || !(slice.get(1) instanceof List)) {
      throw new IllegalArgumentException("Not a valid object");
    }

    return (List<String>) slice.get(1);
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
      String currentPropertyPath, List<Object> slice, Function<String, String> genericLookup) {
    Map<String, Integer> valueIndexes = getValueIndexes(slice);

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

  static Map<String, Integer> getValueIndexes(List<Object> slice) {
    Map<String, Integer> valueIndexes = new HashMap<>();

    for (int i = 0; i < slice.size(); i++) {
      if (slice.get(i) == FeatureTokenType.VALUE) {
        if (i + 2 < slice.size() && slice.get(i + 1) instanceof List) {
          valueIndexes.put(PATH_JOINER.join((List<String>) slice.get(i + 1)), i + 2);
        }
      }
    }

    return valueIndexes;
  }

  static String getValue(List<Object> slice, int valueIndex) {
    if (valueIndex < slice.size() && slice.get(valueIndex) instanceof String) {
      return (String) slice.get(valueIndex);
    }

    return null;
  }
}
