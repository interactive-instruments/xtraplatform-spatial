/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.feature.provider.sql.domain.SchemaMappingSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;

@Modifiable
@Value.Style(deepImmutablesDetection = true)
public interface SqlMutationContext extends ModifiableContext<SchemaSql, SchemaMappingSql> {

}
