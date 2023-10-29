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
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.Tuple;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerObjectReduceSelect
    extends FeaturePropertyTokenSliceTransformer {

  String TYPE = "OBJECT_REDUCE_SELECT";

  @Override
  default String getType() {
    return TYPE;
  }

  @Override
  default FeatureSchema transformSchema(FeatureSchema schema) {
    Optional<FeatureSchema> selected =
        schema.getProperties().stream()
            .filter(property -> Objects.equals(property.getName(), getParameter()))
            .findFirst();

    if (selected.isEmpty()) {
      throw new IllegalArgumentException("Selected property not found: " + getParameter());
    }

    String mergedSourcePath =
        schema.getSourcePath().map(s -> s + "/").orElse("")
            + selected.get().getSourcePath().orElse("");

    ImmutableFeatureSchema build =
        new Builder()
            .from(selected.get())
            .name(schema.getName())
            .sourcePath(mergedSourcePath)
            .path(schema.getPath())
            .parentPath(schema.getParentPath())
            .build();
    return build;
  }

  @Override
  default List<Object> transform(String currentPropertyPath, List<Object> slice) {
    if (slice.isEmpty()) {
      return slice;
    }

    List<String> path = getRootObjectPath(slice);

    Tuple<String, Type> valueAndType =
        getValueAndType(slice, getPropertyPath() + "." + getParameter());

    return valueSlice(path, valueAndType.first(), valueAndType.second());
  }

  static int getValueIndex(List<Object> slice, String path) {
    for (int i = 0; i < slice.size(); i++) {
      if (slice.get(i) == FeatureTokenType.VALUE) {
        if (i + 2 < slice.size() && slice.get(i + 1) instanceof List) {
          if (Objects.equals(path, PATH_JOINER.join((List<String>) slice.get(i + 1)))) {
            return i + 2;
          }
        }
      }
    }

    return -1;
  }

  static String getValue(List<Object> slice, String path) {
    int valueIndex = getValueIndex(slice, path);

    if (valueIndex < 0) {
      return null;
    }

    return (String) slice.get(valueIndex);
  }

  static Tuple<String, Type> getValueAndType(List<Object> slice, String path) {
    int valueIndex = getValueIndex(slice, path);

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
