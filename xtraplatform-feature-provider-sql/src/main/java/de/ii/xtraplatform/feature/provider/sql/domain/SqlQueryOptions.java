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
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Value.Immutable
public interface SqlQueryOptions extends FeatureProviderConnector.QueryOptions {

    static SqlQueryOptions withColumnTypes(Class<?>... columnTypes) {
        return ImmutableSqlQueryOptions.of(Arrays.asList(columnTypes));
    }

    @Value.Parameter
    List<Class<?>> getColumnTypes();

    Optional<FeatureStoreAttributesContainer> getAttributesContainer();

    @Value.Derived
    default boolean isPlain() {
        return !getAttributesContainer().isPresent();
    }

    @Value.Default
    default int getContainerPriority() {
        return 0;
    }
}
