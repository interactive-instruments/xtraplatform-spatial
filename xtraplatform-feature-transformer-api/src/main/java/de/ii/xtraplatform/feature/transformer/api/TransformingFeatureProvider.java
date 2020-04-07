/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider;
import de.ii.xtraplatform.feature.provider.api.FeatureStream;
import de.ii.xtraplatform.features.domain.FeatureTransformer;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureQuery;

/**
 * @author zahnen
 */
public interface TransformingFeatureProvider<T extends FeatureTransformer, U extends FeatureConsumer> extends FeatureProvider<U> {
        FeatureStream<T> getFeatureTransformStream(FeatureQuery query);




    boolean supportsCrs(EpsgCrs crs);

    default boolean shouldSwapCoordinates(EpsgCrs crs) {
        return false;
    }

}
