/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;

import java.io.OutputStream;


//TODO
// e.g. in QueriesHandlerImpl
// Sink<ByteString, CompletionStage<IOResult>> sink = StreamConverters.fromOutputStream((Creator<OutputStream>) () -> outputStream);
// get FeatureEncoder from OutputFormat
// FeatureEncoder.WithSink featureSink = featureEncoder.withSink(sink);
// featureQueries.run(query, featureSink)
// instead of featureProvider.queries().getFeatureStream2(query).runWith(transformer.apply(outputStream))


public interface FeatureEncoder<T, U extends PropertyBase<U,W>, V extends FeatureBase<U,W>, W extends SchemaBase<W>> {

    Flow<V, T, ?> flow(W schema);

    //TODO: performant alternative ???
    // token based, e.g. START_FEATURE, schema, START_OBJECT, schema, START_VALUE, schema, value, END_VALUE, END_OBJECT, END_FEATURE
    // -> separate FeatureTokenEncoder
    //FeatureReaderGeneric encode(OutputStream outputStream);

    default WithSink<U, V, W> withSink(Sink<T, ?> sink) {
        return (schema) -> FeatureEncoder.this.flow(schema).to(sink);
    }

    interface WithSink<U extends PropertyBase<U,W>, V extends FeatureBase<U,W>, W extends SchemaBase<W>> {
        Sink<V, ?> encode(W schema);
    }
}
