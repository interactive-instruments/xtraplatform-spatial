/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FilterEncoder;

public interface FilterEncoderSqlNewNew extends FilterEncoder<String> {
    String encodeNested(CqlFilter cqlFilter, FeatureStoreAttributesContainer typeInfo, boolean isUserFilter);
}
