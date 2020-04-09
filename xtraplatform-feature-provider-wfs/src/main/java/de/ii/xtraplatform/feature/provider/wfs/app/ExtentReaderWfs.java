/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.feature.provider.wfs.domain.WfsConnector;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.Metadata;
import org.codehaus.staxmate.SMInputFactory;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static de.ii.xtraplatform.api.functional.LambdaWithException.mayThrow;

public class ExtentReaderWfs implements ExtentReader {

    private final WfsConnector connector;
    private final Optional<CrsTransformer> crsTransformer;

    public ExtentReaderWfs(WfsConnector connector, CrsTransformerFactory crsTransformerFactory, EpsgCrs nativeCrs) {
        this.connector = connector;
        this.crsTransformer = crsTransformerFactory.getTransformer(OgcCrs.CRS84, nativeCrs);
    }

    @Override
    public RunnableGraph<CompletionStage<Optional<BoundingBox>>> getExtent(FeatureStoreTypeInfo typeInfo) {

        Optional<BoundingBox> boundingBox = connector.getMetadata()
                                                      .map(Metadata::getFeatureTypesBoundingBox)
                                                      .flatMap(boundingBoxes -> Optional.ofNullable(boundingBoxes.get(typeInfo.getName())))
                                                      .flatMap(boundingBox1 -> crsTransformer.map(mayThrow(crsTransformer1 -> crsTransformer1.transformBoundingBox(boundingBox1))));

        return Source.single(boundingBox)
                     .toMat(Sink.head(), Keep.right());
    }
}
