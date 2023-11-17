/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.List;

public interface DynamicTargetSchemaTransformer {

  boolean isApplicableDynamic(List<String> path);

  default List<String> transformPathDynamic(List<String> path) {
    return path;
  }

  List<FeatureSchema> transformSchemaDynamic(List<FeatureSchema> schemas, List<String> indexedPath);
}
