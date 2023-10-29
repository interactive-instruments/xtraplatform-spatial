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
    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .type(Type.STRING)
        .propertyMap(Map.of())
        .build();
  }

  @Override
  default List<Object> transform(String currentPropertyPath, List<Object> slice) {
    if (slice.isEmpty()) {
      return slice;
    }

    List<String> path = getRootObjectPath(slice);

    Function<String, String> lookup =
        getValueLookup(currentPropertyPath, slice, getSubstitutionLookup());

    String value = StringTemplateFilters.applyTemplate(getParameter(), lookup);

    return List.of(FeatureTokenType.VALUE, path, value, Type.STRING);
  }
}
