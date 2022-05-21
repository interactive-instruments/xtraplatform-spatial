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

    Cql2Predicate read(String cql, Format format) throws CqlParseException;

    Cql2Predicate read(String cql, Format format, EpsgCrs crs) throws CqlParseException;

    String write(Cql2Predicate cql, Format format);

    List<String> findInvalidProperties(Cql2Predicate cqlPredicate, Collection<String> validProperties);

    void checkTypes(Cql2Predicate cqlPredicate, Map<String, String> propertyTypes);

    void checkCoordinates(Cql2Predicate cqlPredicate, CrsTransformerFactory crsTransformerFactory, CrsInfo crsInfo, EpsgCrs filterCrs, EpsgCrs nativeCrs);

    Cql2Predicate mapTemporalOperators(Cql2Predicate cqlFilter, Set<TemporalOperator> supportedOperators);

    Cql2Predicate mapEnvelopes(Cql2Predicate cqlFilter, CrsInfo crsInfo);
}
