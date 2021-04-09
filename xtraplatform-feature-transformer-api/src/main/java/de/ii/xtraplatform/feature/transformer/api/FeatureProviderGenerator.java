/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.features.domain.FeatureProviderMetadataConsumer;
import de.ii.xtraplatform.features.domain.FeatureProviderSchemaConsumer;

import java.util.List;

//TODO
public interface FeatureProviderGenerator {
            FeatureProviderMetadataConsumer getDataGenerator(FeatureProviderDataTransformer data,
                                                             ImmutableFeatureProviderDataTransformer.Builder dataBuilder);

            FeatureProviderSchemaConsumer getMappingGenerator(
                    FeatureProviderDataTransformer data,
                    ImmutableFeatureProviderDataTransformer.Builder dataBuilder,
                    List<TargetMappingProviderFromGml> mappingProviders);
    }
