/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

public interface Property<T extends Feature<? extends Property<T>>> {

    FeatureProperty getSchema();

    long getIndex();

    int getDepth();

    Optional<T> getParentFeature();

    Optional<? extends Property<T>> getParentProperty();

    List<? extends Property<T>> getNestedProperties();

    @Value.Default
    default String getName() {
        return getSchema().getName();
    }

    String getValue();


    Property<T> setSchema(FeatureProperty schema);

    Property<T> setName(String name);

    Property<T> setValue(String value);
}
