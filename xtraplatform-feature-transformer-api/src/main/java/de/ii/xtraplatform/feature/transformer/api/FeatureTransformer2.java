/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

/**
 * @author zahnen
 */
public interface FeatureTransformer2 {


    void transform(Source<ByteString, NotUsed> featureStream);
    //Flow<ByteString, ByteString, NotUsed> transform(Source<ByteString, NotUsed> featureStream, StreamingGmlTransformerFlow.GmlTransformerFlow transformerFlow);
}

/*
- request to featureQuery
- service.getProvider
- provider.getFeatureStream(featureQuery)
- service.getFeatureReader
- service.getFeatureWriter(outputFormat)

- transformer.transform(featureStream, writer)
- reader.read(featureStream).writeTo(writer)
- transformer.from(featureStream).with(writer).to(outputStream)
- TransformerBuilder and TransformationRunner
 */

/*
- request to featureQuery
- service.getProvider
- provider.getFeatureStream(featureQuery) returns StreamingFeatureFlow, hides GMLParser
- service.getFeatureWriter(outputFormat) implements featureConsumer
- featureStream.pipeTo(featureConsumer)
- ??? who runs the strean with materializer
- //TODO: first reactivate tests, then refactor
- TargetMapping, SchemaParser etc. to transformer-api
 */