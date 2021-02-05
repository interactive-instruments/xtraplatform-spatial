/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.api;

import akka.Done;
import com.codahale.metrics.Timer;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author zahnen
 */
public interface FeatureStream<T> extends BiFunction<T, Timer.Context, CompletionStage<Done>> {
}
