/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface FeatureNormalizer<T> {

    Sink<T, CompletionStage<FeatureStream2.Result>> normalizeAndTransform(FeatureTransformer2 featureTransformer, FeatureQuery featureQuery);

    <V extends PropertyBase<V,X>, W extends FeatureBase<V,X>, X extends SchemaBase<X>> Source<W, CompletionStage<FeatureStream2.Result>> normalize(Source<T, NotUsed> sourceStream, FeatureQuery featureQuery, Supplier<W> featureCreator, Supplier<V> propertyCreator);

}
