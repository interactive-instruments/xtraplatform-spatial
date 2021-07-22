/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.cql.domain.CqlFilter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface FeatureStoreQueryGenerator<T> {

    T getMetaQuery(FeatureStoreInstanceContainer instanceContainer, int limit, int offset,
        Optional<CqlFilter> cqlFilter,
        List<SortKey> sortKeys,
        boolean computeNumberMatched);

    Stream<T> getInstanceQueries(FeatureStoreInstanceContainer instanceContainer,
        Optional<CqlFilter> cqlFilter,
        List<SortKey> sortKeys, Object minKey,
        Object maxKey, List<Object> customMinKeys, List<Object> customMaxKeys);

    T getExtentQuery(FeatureStoreInstanceContainer instanceContainer, FeatureStoreAttributesContainer attributesContainer);
}
