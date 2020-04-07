/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import akka.stream.javadsl.RunnableGraph;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.wfs.domain.WfsConnector;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class ExtentReaderWfs implements ExtentReader {

    public ExtentReaderWfs(WfsConnector connector, EpsgCrs nativeCrs) {

    }

    @Override
    public RunnableGraph<CompletionStage<Optional<BoundingBox>>> getExtent(FeatureStoreTypeInfo typeInfo) {
        return null;
    }
}
