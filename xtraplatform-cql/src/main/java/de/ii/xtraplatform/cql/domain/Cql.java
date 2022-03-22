/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Cql {

    enum Format {TEXT, JSON}

    CqlFilter read(String cql, Format format) throws CqlParseException;

    CqlFilter read(String cql, Format format, EpsgCrs crs) throws CqlParseException;

    String write(CqlFilter cql, Format format);

    List<String> findInvalidProperties(CqlPredicate cqlPredicate, Collection<String> validProperties);

    void checkTypes(CqlPredicate cqlPredicate, Map<String, String> propertyTypes);

    void checkCoordinates(CqlPredicate cqlPredicate, CrsTransformerFactory crsTransformerFactory, CrsInfo crsInfo, EpsgCrs filterCrs, EpsgCrs nativeCrs);

    CqlNode mapTemporalOperators(CqlFilter cqlFilter, Set<TemporalOperator> supportedOperators);
}
