/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.stream.javadsl.Sink;

import java.util.concurrent.CompletionStage;

public interface FeatureSourceStream<T> {

    CompletionStage<FeatureStream2.Result> runWith(FeatureConsumer consumer);

    CompletionStage<FeatureStream2.Result> runWith2(Sink<T, CompletionStage<FeatureStream2.Result>> consumer);

}
