/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.query.api;

import de.ii.xtraplatform.crs.api.EpsgCrs;

/**
 * @author zahnen
 */
public class FeatureQuery {

    private final String type;
    private final EpsgCrs crs;
    private final int limit;
    private final int offset;
    private final String filter;


    public FeatureQuery(String type, EpsgCrs crs, int limit, int offset, String filter) {
        this.type = type;
        this.crs = crs;
        this.limit = limit;
        this.offset = offset;
        this.filter = filter;
    }

    public String getType() {
        return type;
    }

    public EpsgCrs getCrs() {
        return crs;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getFilter() {
        return filter;
    }
}
