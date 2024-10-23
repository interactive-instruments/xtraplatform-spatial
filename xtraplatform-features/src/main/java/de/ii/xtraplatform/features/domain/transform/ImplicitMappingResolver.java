/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.MappingOperationResolver;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.TypesResolver;
import java.util.List;

public class ImplicitMappingResolver implements TypesResolver {

  @Override
  public boolean needsResolving(FeatureSchema property, boolean isFeature) {
    if (isFeature) {
      return false;
    }

    boolean isFeatureRefInConcat =
        (property.isFeatureRef() && MappingOperationResolver.isConcatPath(property.getPath()));
    boolean isEmptySourcePathInConcat =
        (property.getSourcePath().isPresent()
            && property.getSourcePath().get().indexOf("/", 1) == -1
            && MappingOperationResolver.isConcatPath(property.getPath()));

    return ((property.isObject() || property.isArray())
            && (property.getSourcePath().isEmpty() || isEmptySourcePathInConcat))
        || (property.isObject()
            && property.getSourcePath().isPresent()
            && property.getValueNames().isEmpty())
        || property.getType() == Type.VALUE_ARRAY
        || isFeatureRefInConcat;
  }

  @Override
  public FeatureSchema resolve(FeatureSchema property, List<FeatureSchema> parents) {
    return new Builder()
        .from(property)
        .transformations(List.of())
        .addTransformations(
            new ImmutablePropertyTransformation.Builder().wrap(property.getType()).build())
        .addAllTransformations(property.getTransformations())
        .build();
  }
}
