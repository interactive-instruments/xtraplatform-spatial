/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public interface FeatureStoreAttribute {

    String getName();

    List<String> getPath();

    @Value.Default
    default String getQueryable() {
        return getName().replaceAll("\\[", "").replaceAll("\\]", "");
    }

    @Value.Default
    default boolean isId() {
        return false;
    }

    @Value.Default
    default boolean isSpatial() {
        return false;
    }

    @Value.Default
    default boolean isTemporal() {
        return false;
    }

    Optional<String> getConstantValue();

    @Value.Derived
    default boolean isConstant() {
        return getConstantValue().isPresent();
    }
}
