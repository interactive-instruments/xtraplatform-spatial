/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.domain.PropertyBase;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Style(set = "*")
public interface PropertySql extends PropertyBase<PropertySql, SchemaSql>, ObjectSql {

}
