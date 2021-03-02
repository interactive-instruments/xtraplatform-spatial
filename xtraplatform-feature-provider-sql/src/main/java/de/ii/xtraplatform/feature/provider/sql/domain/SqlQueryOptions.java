/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.SortKey;
import de.ii.xtraplatform.features.domain.SortKey.Direction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;

@Value.Immutable
public interface SqlQueryOptions extends FeatureProviderConnector.QueryOptions {

  static SqlQueryOptions withColumnTypes(Class<?>... columnTypes) {
    return withColumnTypes(Arrays.asList(columnTypes));
  }

  static SqlQueryOptions withColumnTypes(List<Class<?>> columnTypes) {
    return ImmutableSqlQueryOptions.builder().customColumnTypes(columnTypes).build();
  }

  Optional<FeatureStoreAttributesContainer> getAttributesContainer();

  List<SortKey> getCustomSortKeys();

  List<Class<?>> getCustomColumnTypes();

  @Value.Default
  default int getContainerPriority() {
    return 0;
  }

  @Value.Derived
  default List<String> getSortKeys() {
    return Stream
        .concat(getCustomSortKeys().stream().map(SortKey::getField), getAttributesContainer()
            .map(attributesContainer -> attributesContainer.getSortKeys().stream())
            .orElse(Stream.empty())).collect(
            Collectors.toList());
  }

  @Value.Derived
  default List<SortKey.Direction> getSortDirections() {
    return Stream
        .concat(getCustomSortKeys().stream().map(SortKey::getDirection), getAttributesContainer()
            .map(attributesContainer -> attributesContainer.getSortKeys().stream()
                .map(s -> Direction.ASCENDING))
            .orElse(Stream.empty())).collect(
            Collectors.toList());
  }

  @Value.Derived
  default List<Class<?>> getColumnTypes() {
    List<Class<?>> columnTypes = new ArrayList<>();

    getAttributesContainer().ifPresent(attributesContainer -> attributesContainer.getAttributes()
        .forEach(attribute -> columnTypes.add(String.class)));

    columnTypes.addAll(getCustomColumnTypes());

    return columnTypes;
  }

  @Value.Derived
  default boolean isPlain() {
    return !getAttributesContainer().isPresent();
  }
}
