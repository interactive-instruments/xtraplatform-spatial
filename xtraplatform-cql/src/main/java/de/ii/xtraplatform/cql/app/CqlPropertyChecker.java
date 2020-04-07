/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.Property;

import java.util.ArrayList;
import java.util.List;

public class CqlPropertyChecker extends CqlVisitorBase<List<String>> {

    private final List<String> allowedProperties;
    private final List<String> invalidProperties = new ArrayList<>();

    public CqlPropertyChecker(List<String> allowedProperties) {
        this.allowedProperties = allowedProperties;
    }

    @Override
    public List<String> visit(CqlFilter cqlFilter, List<List<String>> children) {
        ImmutableList<String> result = ImmutableList.copyOf(invalidProperties);
        invalidProperties.clear();
        return result;
    }

    @Override
    public List<String> visit(Property property, List<List<String>> children) {
        String propertyName = property.getName();
        if (!allowedProperties.contains(propertyName)) {
            invalidProperties.add(propertyName);
        }
        return Lists.newArrayList();
    }

}
