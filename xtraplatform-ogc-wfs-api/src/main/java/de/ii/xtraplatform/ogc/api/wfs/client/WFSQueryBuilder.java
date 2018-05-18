/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ogc.api.wfs.client;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.opengis.filter.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WFSQueryBuilder {
    private final List<String> typeNames;
    private List<Filter> filter;
    private EpsgCrs crs;

    public WFSQueryBuilder() {
        this.typeNames = new ArrayList<>();
        this.filter = new ArrayList<>();
    }

    public WFSQueryBuilder typeName(String typeName) {
        this.typeNames.add(typeName);
        return this;
    }

    public WFSQueryBuilder filter(Filter filter) {
        this.filter.add(filter);
        return this;
    }

    public WFSQueryBuilder crs(EpsgCrs crs) {
        this.crs = crs;
        return this;
    }

    public WFSQuery2 build() {
        final List<String> types = ImmutableList.copyOf(typeNames);
        final WFSQuery2 query = new WFSQuery2(types, ImmutableList.copyOf(filter), crs);

        // WFSQuery2 has no getters, so we pass the members to validate
        validate(query, types);

        return query;
    }

    private void validate(final WFSQuery2 query, final List<String> types) {
        Objects.requireNonNull(query, "Query may not be null");
        Objects.requireNonNull(types, "At least on type name is required");
        if (types.isEmpty()) {
            throw new IllegalStateException("At least on type name is required");
        }
    }
}