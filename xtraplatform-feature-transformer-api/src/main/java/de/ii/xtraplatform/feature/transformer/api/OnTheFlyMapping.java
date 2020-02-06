/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.features.domain.legacy.TargetMapping;

import java.util.List;

/**
 * @author zahnen
 */
public interface OnTheFlyMapping {
    TargetMapping getTargetMappingForFeatureType(String path);

    TargetMapping getTargetMappingForAttribute(List<String> path, String value);

    TargetMapping getTargetMappingForProperty(List<String> path, String value);

    TargetMapping getTargetMappingForGeometry(List<String> path);
}
