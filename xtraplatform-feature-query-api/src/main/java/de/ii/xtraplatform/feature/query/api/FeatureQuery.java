/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.query.api;

import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * @author zahnen
 */
@Value.Immutable
public abstract class FeatureQuery {

    public abstract String getType();

    @Nullable
    public abstract EpsgCrs getCrs();

    @Value.Default
    public int getLimit() {return 0;}

    @Value.Default
    public int getOffset() {return 0;};

    @Nullable
    public abstract String getFilter();

    @Value.Default
    public boolean hitsOnly() {return false;}

    @Value.Default
    public double getMaxAllowableOffset() {
        return 0;
    }
}
