/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FeatureStoreInsertGenerator {
  Function<FeatureSql, Tuple<String, Consumer<String>>> createInsert(
      SchemaSql schema, List<Integer> parentRows, Optional<String> id, EpsgCrs crs);

  Function<FeatureSql, Tuple<String, Consumer<String>>> createJunctionInsert(
      SchemaSql schema, List<Integer> parentRows);

  Function<FeatureSql, Tuple<String, Consumer<String>>> createForeignKeyUpdate(
      SchemaSql schema, List<Integer> parentRows);
}
