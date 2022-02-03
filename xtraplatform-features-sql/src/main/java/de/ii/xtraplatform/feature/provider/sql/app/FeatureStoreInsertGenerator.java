/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import akka.japi.Pair;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FeatureStoreInsertGenerator {
    Function<FeatureSql, Pair<String, Consumer<String>>> createInsert(
            SchemaSql schema, List<Integer> parentRows, Optional<String> id);

    Function<FeatureSql, Pair<String, Consumer<String>>> createJunctionInsert(
            SchemaSql schema, List<Integer> parentRows);

    Function<FeatureSql, Pair<String, Consumer<String>>> createForeignKeyUpdate(
            SchemaSql schema, List<Integer> parentRows);
}
