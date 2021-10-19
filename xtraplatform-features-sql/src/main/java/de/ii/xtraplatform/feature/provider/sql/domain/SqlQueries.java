/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Value.Immutable
public interface SqlQueries {

    Optional<String> getMetaQuery();

    Function<SqlRowMeta, Stream<String>> getValueQueries();

    List<FeatureStoreInstanceContainer> getInstanceContainers();

    List<SchemaSql> getTableSchemas();
}
