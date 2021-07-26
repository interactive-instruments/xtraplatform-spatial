/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerRename extends FeaturePropertySchemaTransformer {

    String TYPE = "RENAME";

    @Override
    default String getType() {
        return TYPE;
    }

    @Override
    default FeatureSchema transform(FeatureSchema input) {
        return new ImmutableFeatureSchema.Builder().from(input)
                                                     .name(getParameter())
                                                     .build();
    }
}
