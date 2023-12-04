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
import java.util.function.Function;
import org.immutables.value.Value;

@Value.Immutable
public abstract class FeaturePropertyTransformerArrayReduceFormat
    implements FeaturePropertyTokenSliceTransformer {

  private static final String TYPE = "ARRAY_REDUCE_FORMAT";

  private FeatureSchema schema;

  @Override
  public String getType() {
    return TYPE;
  }

  public abstract Function<String, String> getSubstitutionLookup();

  @Override
  public FeatureSchema transformSchema(FeatureSchema schema) {
    checkValueArray(schema);

    this.schema = schema;

    return FeatureSchema.apply(
        schema, builder -> builder.type(Type.VALUE).valueType(Type.STRING).concat(List.of()));
  }

  @Override
  public List<Object> transform(String currentPropertyPath, List<Object> slice) {
    return transformValueArray(pathAsList(currentPropertyPath), slice);
  }

  @Override
  public void transformValue(List<Object> slice, int start, int end, List<Object> result) {
    Function<String, String> lookup =
        getValueIndexLookup(schema.getFullPath(), getSubstitutionLookup(), slice, start, end);

    String value = StringTemplateFilters.applyTemplate(getParameter(), lookup);

    result.addAll(valueSlice(schema.getFullPath(), value, Type.STRING));
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
