/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;

@AutoMultiBind
public interface SchemaFragmentResolver {

  boolean canResolve(String ref, FeatureProviderDataV2 data);

  FeatureSchema resolve(String ref, FeatureSchema original, FeatureProviderDataV2 data);

  PartialObjectSchema resolve(String ref, PartialObjectSchema original, FeatureProviderDataV2 data);
}
