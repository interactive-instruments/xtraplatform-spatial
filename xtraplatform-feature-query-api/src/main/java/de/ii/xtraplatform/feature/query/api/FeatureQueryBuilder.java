/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.query.api;

import de.ii.xtraplatform.crs.api.EpsgCrs;

public class FeatureQueryBuilder {
    private String type;
    private EpsgCrs crs;
    private int limit;
    private int offset;
    private String filter;

    public FeatureQueryBuilder type(String type) {
        this.type = type;
        return this;
    }

    public FeatureQueryBuilder crs(EpsgCrs crs) {
        this.crs = crs;
        return this;
    }

    public FeatureQueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public FeatureQueryBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }

    public FeatureQueryBuilder filter(String filter) {
        this.filter = filter;
        return this;
    }

    public FeatureQuery build() {
        return new FeatureQuery(type, crs, limit, offset, filter);
    }
}