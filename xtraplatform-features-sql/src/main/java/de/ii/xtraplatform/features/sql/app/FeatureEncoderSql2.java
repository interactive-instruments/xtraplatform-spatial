/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureObjectTransformerBase;
import de.ii.xtraplatform.features.domain.FeatureTokenEmitter2;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.features.sql.domain.SchemaMappingSql;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import java.util.Optional;

public class FeatureEncoderSql2
    extends FeatureObjectTransformerBase<SchemaSql, SchemaMappingSql, PropertySql, FeatureSql> {

  private final SchemaMappingSql mapping;

  public FeatureEncoderSql2(SchemaMappingSql mapping) {
    this(mapping, Optional.empty());
  }

  public FeatureEncoderSql2(SchemaMappingSql mapping, Optional<String> nullValue) {
    super(nullValue);
    this.mapping = mapping;
  }

  @Override
  public FeatureSql createFeature() {
    return ModifiableFeatureSql.create();
  }

  @Override
  public PropertySql createProperty() {
    return ModifiablePropertySql.create();
  }

  @Override
  public void onFeature(FeatureSql feature) {
    boolean br = true;
    if (getDownstream() instanceof FeatureTokenEmitter2) {
      ((FeatureTokenEmitter2<?, ?, ?>) getDownstream()).push(feature);
    }
  }

  @Override
  public Class<? extends ModifiableContext<SchemaSql, SchemaMappingSql>> getContextInterface() {
    return SqlMutationContext.class;
  }

  @Override
  public ModifiableContext<SchemaSql, SchemaMappingSql> createContext() {
    return ModifiableSqlMutationContext.create()
        .setType(mapping.getTargetSchema().getName())
        .setMappings(ImmutableMap.of(mapping.getTargetSchema().getName(), mapping))
        .setQuery(ImmutableFeatureQuery.builder().type("xyz").build());
  }
}
