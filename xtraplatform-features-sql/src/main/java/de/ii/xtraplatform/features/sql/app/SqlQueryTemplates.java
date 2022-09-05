/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.features.domain.SortKey;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface SqlQueryTemplates {

  MetaQueryTemplate getMetaQueryTemplate();

  List<ValueQueryTemplate> getValueQueryTemplates();

  List<SchemaSql> getQuerySchemas();

  @FunctionalInterface
  interface MetaQueryTemplate {
    String generateMetaQuery(
        long limit,
        long offset,
        long skipOffset,
        List<SortKey> additionalSortKeys,
        Optional<Cql2Expression> filter,
        Map<String, String> virtualTables,
        boolean withNumberSkipped,
        boolean withNumberReturned);
  }

  @FunctionalInterface
  interface ValueQueryTemplate {
    String generateValueQuery(
        long limit,
        long offset,
        List<SortKey> additionalSortKeys,
        Optional<Cql2Expression> filter,
        Optional<Tuple<Object, Object>> minMaxKeys,
        Map<String, String> virtualTables);
  }
}
