/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.akka.http.AkkaHttp;
import de.ii.xtraplatform.feature.query.api.FeatureConsumer;
import de.ii.xtraplatform.feature.query.api.FeatureProvider;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureStream;

/**
 * @author zahnen
 */
public interface TransformingFeatureProvider<T extends FeatureTransformer, U extends FeatureConsumer> extends FeatureProvider<U> {
        FeatureStream<T> getFeatureTransformStream(FeatureQuery query, AkkaHttp akkaHttp);
}
