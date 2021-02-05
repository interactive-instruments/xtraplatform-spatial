/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.stream.javadsl.RunnableGraph;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.streams.domain.RunnableGraphWithMdc;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface ExtentReader {
    RunnableGraphWithMdc<CompletionStage<Optional<BoundingBox>>> getExtent(FeatureStoreTypeInfo typeInfo);
}
