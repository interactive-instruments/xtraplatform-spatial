/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import de.ii.xtraplatform.features.sql.domain.ImmutableSchemaSql;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OnlyReturnables implements SchemaVisitorTopDown<SchemaSql, SchemaSql> {

  public OnlyReturnables() {}

  @Override
  public SchemaSql visit(
      SchemaSql schema, List<SchemaSql> parents, List<SchemaSql> visitedProperties) {

    if (schema.returnable()) {
      return new ImmutableSchemaSql.Builder()
          .from(schema)
          .properties(
              visitedProperties.stream().filter(Objects::nonNull).collect(Collectors.toList()))
          .build();
    }

    return null;
  }
}
