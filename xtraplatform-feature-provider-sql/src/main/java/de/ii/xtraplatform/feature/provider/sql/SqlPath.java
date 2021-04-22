/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

@Value.Immutable
public interface SqlPath {

    @Nullable
    SqlPath getParent();

    String getTablePath();

    List<String> getColumns();

    @Value.Default
    default boolean isRoot() {
        return false;
    }

    @Value.Default
    default boolean isJunction() {
        return false;
    }

    @Value.Default
    default boolean hasOid() {
        return false;
    }

    OptionalInt getSortPriority();

    String getQueryable();

    @Value.Default
    default boolean isSpatial() {
        return false;
    }

    @Value.Default
    default boolean isTemporal() {
        return false;
    }

    //String getSortKey();

    Optional<String> getConstantValue();

    Map<String, String> getTableFlags();
}
