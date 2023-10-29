/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.List;

public interface FeaturePropertyTokenSliceTransformer
    extends FeaturePropertyTransformer<List<Object>> {

  FeatureSchema transformSchema(FeatureSchema schema);
}
