/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.immutables.value.Value;

@Value.Immutable
public interface SqlQuerySet {

  Function<Long, Optional<String>> getMetaQuery();

  Function<SqlRowMeta, Stream<String>> getValueQueries();

  List<SchemaSql> getTableSchemas();

  SqlQueryOptions getOptions();
}
