/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WithTransformationsApplied
    implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  private final PropertyTransformations additionalTransformations;
  private final boolean preferSchemaTransformations;

  public WithTransformationsApplied() {
    this(new LinkedHashMap<>());
  }

  public WithTransformationsApplied(Map<String, PropertyTransformation> additionalTransformations) {
    this(
        () ->
            additionalTransformations.entrySet().stream()
                .map(entry -> new SimpleEntry<>(entry.getKey(), ImmutableList.of(entry.getValue())))
                .collect(
                    ImmutableMap.toImmutableMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) ->
                            ImmutableList.<PropertyTransformation>builder()
                                .addAll(first)
                                .addAll(second)
                                .build())),
        true);
  }

  public WithTransformationsApplied(PropertyTransformations additionalTransformations) {
    this(additionalTransformations, false);
  }

  public WithTransformationsApplied(
      PropertyTransformations additionalTransformations, boolean preferSchemaTransformations) {
    this.additionalTransformations = additionalTransformations;
    this.preferSchemaTransformations = preferSchemaTransformations;
  }

  public Optional<String> getFlatteningSeparator(FeatureSchema schema) {
    return getFeatureTransformations(schema).flatMap(PropertyTransformation::getFlatten);
  }

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {
    if (!parents.isEmpty()) {
      return schema;
    }

    SchemaTransformerChain schemaTransformations =
        getPropertyTransformations(schema).getSchemaTransformations(null, true);

    TokenSliceTransformerChain sliceTransformations =
        getPropertyTransformations(schema).getTokenSliceTransformations(null);

    return schema.accept(schemaTransformations).accept(sliceTransformations);
  }

  private Optional<PropertyTransformation> getFeatureTransformations(FeatureSchema schema) {
    PropertyTransformations schemaTransformations =
        () -> ImmutableMap.of(PropertyTransformations.WILDCARD, schema.getTransformations());

    PropertyTransformations mergedTransformations =
        preferSchemaTransformations
            ? schemaTransformations.mergeInto(additionalTransformations)
            : additionalTransformations.mergeInto(schemaTransformations);

    List<PropertyTransformation> featureTransformations =
        mergedTransformations.getTransformations().get(PropertyTransformations.WILDCARD);

    return Optional.ofNullable(featureTransformations)
        .filter(list -> !list.isEmpty())
        .map(list -> list.get(list.size() - 1));
  }

  private PropertyTransformations getPropertyTransformations(FeatureSchema schema) {
    PropertyTransformations schemaTransformations =
        () -> ImmutableMap.of(schema.getFullPathAsString(), schema.getTransformations());

    PropertyTransformations mergedTransformations =
        preferSchemaTransformations
            ? schemaTransformations.mergeInto(additionalTransformations)
            : additionalTransformations.mergeInto(schemaTransformations);

    return mergedTransformations;
  }
}
