/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.mayThrow;

import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureMetadata;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.Metadata;
import de.ii.xtraplatform.streams.domain.LogContextStream;
import de.ii.xtraplatform.streams.domain.RunnableGraphWithMdc;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class ExtentReaderWfs implements ExtentReader {

    private final FeatureMetadata featureMetadata;
    private final Optional<CrsTransformer> crsTransformer;

    public ExtentReaderWfs(FeatureMetadata featureMetadata, CrsTransformerFactory crsTransformerFactory, EpsgCrs nativeCrs) {
        this.featureMetadata = featureMetadata;
        this.crsTransformer = crsTransformerFactory.getTransformer(OgcCrs.CRS84, nativeCrs);
    }

    @Override
    public RunnableGraphWithMdc<CompletionStage<Optional<BoundingBox>>> getExtent(FeatureStoreTypeInfo typeInfo) {

        Optional<BoundingBox> boundingBox = featureMetadata.getMetadata()
                                                      .map(Metadata::getFeatureTypesBoundingBox)
                                                      .flatMap(boundingBoxes -> Optional.ofNullable(boundingBoxes.get(typeInfo.getName())))
                                                      .flatMap(boundingBox1 -> crsTransformer.map(mayThrow(crsTransformer1 -> crsTransformer1.transformBoundingBox(boundingBox1))));

        return LogContextStream.graphWithMdc(Source.single(boundingBox), Sink.head());
    }
}
