/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import akka.Done;
import akka.stream.Attributes;
import akka.stream.Inlet;
import akka.stream.SinkShape;
import akka.stream.javadsl.Sink;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.GraphStageLogic;
import akka.stream.stage.GraphStageWithMaterializedValue;
import akka.util.ByteString;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import javax.xml.namespace.QName;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author zahnen
 */
public class GmlStreamParser {

    public static Sink<ByteString, CompletionStage<Done>> consume(final QName featureType, final GmlConsumer gmlConsumer) {
        return Sink.fromGraph(new FeatureSinkFromGml(featureType, gmlConsumer));
    }

    public static Sink<ByteString, CompletionStage<Done>> transform(final QName featureType, final FeatureTypeMapping featureTypeMapping, final FeatureTransformer featureTransformer, List<String> fields) {
        return GmlStreamParser.consume(featureType, new FeatureTransformerFromGml(featureTypeMapping, featureTransformer, fields));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GmlStreamParser.class);

    static class FeatureSinkFromGml extends GraphStageWithMaterializedValue<SinkShape<ByteString>, CompletionStage<Done>> {

        public final Inlet<ByteString> in = Inlet.create("FeatureSinkFromGml.in");
        private final SinkShape<ByteString> shape = SinkShape.of(in);

        private final QName featureType;
        private final GmlConsumer gmlConsumer;

        FeatureSinkFromGml(QName featureType, GmlConsumer gmlConsumer) {
            this.featureType = featureType;
            this.gmlConsumer = gmlConsumer;
        }

        @Override
        public SinkShape<ByteString> shape() {
            return shape;
        }

        @Override
        public Tuple2<GraphStageLogic, CompletionStage<Done>> createLogicAndMaterializedValue(Attributes inheritedAttributes) throws Exception {
            CompletableFuture<Done> promise = new CompletableFuture<>();

            GraphStageLogic logic = new AbstractStreamingGmlGraphStage(shape, featureType, gmlConsumer) {

                @Override
                public void preStart() throws Exception {
                    super.preStart();
                    pull(in);
                }

                {
                    setHandler(in, new AbstractInHandler() {
                        @Override
                        public void onPush() throws Exception {
                            try {
                                byte[] bytes = grab(in).toArray();

                                if (LOGGER.isTraceEnabled()) {
                                    LOGGER.trace(new String(bytes, StandardCharsets.UTF_8));
                                }

                                parser.getInputFeeder()
                                      .feedInput(bytes, 0, bytes.length);
                                boolean feedMeMore = false;
                                while (!feedMeMore && parser.hasNext()) {
                                    feedMeMore = advanceParser();
                                }
                                if (feedMeMore && !isClosed(in)) {
                                    pull(in);
                                }
                            } catch (Exception e) {
                                promise.completeExceptionally(e);
                            }
                        }

                        @Override
                        public void onUpstreamFinish() throws Exception {
                            try {
                                parser.getInputFeeder().endOfInput();
                                while (parser.hasNext())
                                    advanceParser();
                            } catch (Exception e) {
                                promise.completeExceptionally(e);
                            }

                            completeStage();
                            promise.complete(Done.getInstance());
                        }

                        @Override
                        public void onUpstreamFailure(Throwable ex) throws Exception {
                            super.onUpstreamFailure(ex);
                        }
                    });
                }
            };

            return new Tuple2<>(logic, promise);
        }
    }
}
