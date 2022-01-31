/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class ContextTransformerChain implements
    TransformerChain<ModifiableContext, FeaturePropertyContextTransformer> {

  public static final String OBJECT_TYPE_WILDCARD = "*{objectType=";

  private final List<String> currentParentProperties;
  private final Map<String, List<FeaturePropertyContextTransformer>> transformers;

  public ContextTransformerChain(
      Map<String, List<PropertyTransformation>> allTransformations,
      SchemaMapping schemaMapping) {
    this.currentParentProperties = new ArrayList<>();
    this.transformers = allTransformations.entrySet().stream()
        .flatMap(entry -> {
          String propertyPath = entry.getKey();
          List<PropertyTransformation> transformation = entry.getValue();

          if (hasWildcard(propertyPath, OBJECT_TYPE_WILDCARD)) {
              return createContextTransformersForObjectType(propertyPath, schemaMapping, transformation).entrySet().stream();
          }

          return Stream.of(new SimpleEntry<>(propertyPath, createContextTransformers(propertyPath, transformation)));
        })
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> new ImmutableList.Builder<FeaturePropertyContextTransformer>().addAll(first).addAll(second).build()));
  }

  @Nullable
  @Override
  public ModifiableContext transform(String path, ModifiableContext context) {
    boolean ran = false;
    for (int i = currentParentProperties.size() - 1; i >= 0; i--) {
      String parentPath = currentParentProperties.get(i);

      if (!path.startsWith(parentPath + ".")) {
        currentParentProperties.remove(i);
      } else if (!ran) {
        ran = run(transformers, parentPath, path, context);
      }
    }

    if (context.currentSchema().filter(SchemaBase::isObject).isPresent()
        && (currentParentProperties.isEmpty() || !Objects.equals(
        currentParentProperties.get(currentParentProperties.size() - 1), path))) {
      currentParentProperties.add(path);
    }

    if (!ran) {
      run(transformers, path, path, context);
    }

    return context;
  }

  @Override
  public boolean has(String path) {
    return transformers.containsKey(path);
  }

  @Override
  public List<FeaturePropertyContextTransformer> get(String path) {
    return transformers.get(path);
  }

  private boolean run(
      Map<String, List<FeaturePropertyContextTransformer>> contextTransformers, String keyPath,
      String propertyPath, ModifiableContext context) {
    boolean ran = false;

    if (contextTransformers.containsKey(keyPath) && !contextTransformers.get(keyPath)
        .isEmpty()) {
      for (FeaturePropertyContextTransformer contextTransformer : contextTransformers.get(
          keyPath)) {
        ModifiableContext transformed = contextTransformer.transform(propertyPath, context);
        ran = true;

        if (Objects.isNull(transformed)) {
          return ran;
        }
      }
    } else if (contextTransformers.containsKey(PropertyTransformations.WILDCARD)
        && !contextTransformers.get(
            PropertyTransformations.WILDCARD)
        .isEmpty()) {
      for (FeaturePropertyContextTransformer contextTransformer : contextTransformers.getOrDefault(
          PropertyTransformations.WILDCARD,
          ImmutableList.of())) {
        ModifiableContext transformed = contextTransformer.transform(propertyPath, context);
        ran = true;

        if (Objects.isNull(transformed)) {
          return ran;
        }
      }
    }

    return ran;
  }

  private List<FeaturePropertyContextTransformer> createContextTransformers(String path,
      List<PropertyTransformation> propertyTransformations) {
    List<FeaturePropertyContextTransformer> transformers = new ArrayList<>();

    propertyTransformations.forEach(propertyTransformation -> {
      propertyTransformation.getReduceStringFormat()
          .ifPresent(ignore -> transformers
              .add(ImmutableFeaturePropertyTransformerObjectReduce.builder()
                  .propertyPath(path)
                  .parameter("")
                  .build()));
    });

    return transformers;
  }

  private Map<String, List<FeaturePropertyContextTransformer>> createContextTransformersForObjectType(String transformationKey, SchemaMapping schemaMapping, List<PropertyTransformation> propertyTransformation) {
    return explodeWildcard(transformationKey, OBJECT_TYPE_WILDCARD, schemaMapping, ContextTransformerChain::matchesObjectType)
        .stream()
        .map(propertyPath -> new SimpleEntry<>(propertyPath,
            createContextTransformers(propertyPath, propertyTransformation)))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  static boolean matchesObjectType(FeatureSchema schema, String objectType) {
    return schema.getObjectType().isPresent()
        && Objects.equals(schema.getObjectType().get(), objectType);
  }

}
