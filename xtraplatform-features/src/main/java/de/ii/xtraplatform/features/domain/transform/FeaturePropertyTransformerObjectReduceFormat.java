/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.strings.domain.StringTemplateFilters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerObjectReduceFormat
    extends FeaturePropertyTokenSliceTransformer {

  String TYPE = "OBJECT_REDUCE_FORMAT";

  @Override
  default String getType() {
    return TYPE;
  }

  Function<String, String> getSubstitutionLookup();

  @Override
  default FeatureSchema transformSchema(FeatureSchema schema) {
    checkObject(schema);

    return schema.with(
        builder ->
            builder
                .type(schema.isArray() ? Type.VALUE_ARRAY : Type.VALUE)
                .valueType(Type.STRING)
                .propertyMap(Map.of()));
  }

  @Override
  default void transformObject(
      String currentPropertyPath,
      List<Object> slice,
      List<String> rootPath,
      int start,
      int end,
      List<Object> result) {
    Function<String, String> lookup =
        getValueLookup(currentPropertyPath, getSubstitutionLookup(), slice, start, end);

    String value = StringTemplateFilters.applyTemplate(getParameter(), lookup);

    result.addAll(valueSlice(rootPath, value, Type.STRING));
  }
}
