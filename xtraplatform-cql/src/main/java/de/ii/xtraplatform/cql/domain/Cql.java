/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import java.util.Collection;
import java.util.List;

public interface Cql {

    enum Format {TEXT, JSON}

    CqlFilter read(String cql, Format format) throws CqlParseException;

    String write(CqlFilter cql, Format format);

    List<String> findInvalidProperties(CqlPredicate cqlPredicate, Collection<String> validProperties);

}
