/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.SchemaBase;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
public interface SchemaSql extends SchemaBase<SchemaSql> {

  List<SqlRelation> getRelation();

  Optional<String> getPrimaryKey();

  Optional<String> getSortKey();

  Optional<CqlFilter> getFilter();

  @Override
  @Value.Auxiliary
  @Value.Derived
  default List<String> getPath() {
    return getRelation().isEmpty()
        ? ImmutableList.of(getName())
        : getRelation().stream()
            .flatMap(featureStoreRelation -> featureStoreRelation.asPath().stream())
            .collect(Collectors.toList());
  }
}
