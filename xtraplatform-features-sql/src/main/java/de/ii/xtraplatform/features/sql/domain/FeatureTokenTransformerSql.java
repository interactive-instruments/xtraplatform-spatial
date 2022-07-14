/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureTokenContext;
import de.ii.xtraplatform.features.domain.FeatureTokenTransformerBase;
import de.ii.xtraplatform.features.sql.app.ModifiableSqlMutationContext;
import de.ii.xtraplatform.features.sql.app.SqlMutationContext;
import java.util.Objects;

public abstract class FeatureTokenTransformerSql
    extends FeatureTokenTransformerBase<
        SchemaSql, SchemaMappingSql, ModifiableContext<SchemaSql, SchemaMappingSql>> {

  private ModifiableContext<SchemaSql, SchemaMappingSql> context;

  @Override
  public Class<? extends ModifiableContext<SchemaSql, SchemaMappingSql>> getContextInterface() {
    if (getDownstream() instanceof FeatureTokenContext<?>) {
      return ((FeatureTokenContext<ModifiableContext<SchemaSql, SchemaMappingSql>>) getDownstream())
          .getContextInterface();
    }

    return SqlMutationContext.class;
  }

  @Override
  public final ModifiableContext<SchemaSql, SchemaMappingSql> createContext() {
    ModifiableContext<SchemaSql, SchemaMappingSql> context =
        getDownstream() instanceof FeatureTokenContext<?>
            ? ((FeatureTokenContext<ModifiableContext<SchemaSql, SchemaMappingSql>>)
                    getDownstream())
                .createContext()
            : ModifiableSqlMutationContext.create();

    if (Objects.isNull(this.context)) {
      this.context = context;
    }

    return context;
  }

  protected final ModifiableContext<SchemaSql, SchemaMappingSql> getContext() {
    if (Objects.isNull(context)) {
      return createContext();
    }

    return context;
  }
}
