/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.api;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.geometries.domain.EpsgCrs;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
public abstract class FeatureQuery {

    public abstract String getType();

    @Nullable
    public abstract EpsgCrs getCrs();

    @Value.Default
    public int getLimit() {
        return 0;
    }

    @Value.Default
    public int getOffset() {
        return 0;
    }

    ;

    @Nullable
    public abstract String getFilter();

    @Value.Default
    public boolean hitsOnly() {
        return false;
    }

    @Value.Default
    public boolean propertyOnly() {
        return false;
    }

    @Value.Default
    public double getMaxAllowableOffset() {
        return 0;
    }

    @Value.Default
    public int getGeometryPrecision() {
        return 0;
    }

    @Value.Default
    public List<String> getFields() {
        return ImmutableList.of("*");
    }

    @Value.Derived
    public boolean hasIdFilter() {
        return Optional.ofNullable(getFilter())
                       .filter(filter -> filter.matches("IN ('.+?')"))
                       .isPresent();
    }
}
