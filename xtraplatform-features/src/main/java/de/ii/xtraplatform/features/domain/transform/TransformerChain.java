/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public interface TransformerChain<T, U> {

  @Nullable
  T transform(String path, T schema);

  boolean has(String path);

  List<U> get(String path);

  default boolean hasWildcard(String propertyPath, String wildcardParameter) {
    return propertyPath.startsWith(wildcardParameter) && propertyPath.length() > wildcardParameter.length() + 1;
  }

  default String extractWildcardParameter(String propertyPath, String wildcardParameter) {
    return propertyPath.substring(wildcardParameter.length(), propertyPath.length() - 1);
  }

  default List<String> explodeWildcard(String transformationKey, String wildcardPattern,
      SchemaMapping schemaMapping, BiPredicate<FeatureSchema, String> filter) {
    if (!hasWildcard(transformationKey, wildcardPattern)) {
      return ImmutableList.of();
    }

    return schemaMapping.getTargetSchemasByPath()
        .entrySet().stream()
        .filter(entry -> entry.getValue().stream()
            .anyMatch(schema -> filter.test(schema,
                extractWildcardParameter(transformationKey, wildcardPattern))))
        .map(entry -> String.join(".", entry.getKey()))
        .collect(Collectors.toList());
  }
}
