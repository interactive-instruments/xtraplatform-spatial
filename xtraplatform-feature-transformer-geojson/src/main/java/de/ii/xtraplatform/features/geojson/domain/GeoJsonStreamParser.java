/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.geojson.domain;

import akka.Done;
import akka.stream.Attributes;
import akka.stream.Inlet;
import akka.stream.SinkShape;
import akka.stream.javadsl.Sink;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.GraphStageLogic;
import akka.stream.stage.GraphStageWithMaterializedValue;
import akka.util.ByteString;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.features.geojson.app.AbstractStreamingGeoJsonGraphStage;
import de.ii.xtraplatform.features.geojson.app.FeatureTransformerFromGeoJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author zahnen
 */
public class GeoJsonStreamParser {

    public static Sink<ByteString, CompletionStage<Done>> consume(final FeatureConsumer featureConsumer) {
        return Sink.fromGraph(new FeatureSinkFromGeoJson(featureConsumer));
    }

    public static Sink<ByteString, CompletionStage<Done>> transform(final FeatureTypeMapping featureTypeMapping, final FeatureTransformer featureTransformer) {
        return GeoJsonStreamParser.consume(new FeatureTransformerFromGeoJson(featureTypeMapping, featureTransformer));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonStreamParser.class);

    static class FeatureSinkFromGeoJson extends GraphStageWithMaterializedValue<SinkShape<ByteString>, CompletionStage<Done>> {

        public final Inlet<ByteString> in = Inlet.create("FeatureSinkFromGeoJson.in");
        private final SinkShape<ByteString> shape = SinkShape.of(in);

        private final FeatureConsumer featureConsumer;

        FeatureSinkFromGeoJson(FeatureConsumer featureConsumer) {
            this.featureConsumer = featureConsumer;
        }

        @Override
        public SinkShape<ByteString> shape() {
            return shape;
        }

        @Override
        public Tuple2<GraphStageLogic, CompletionStage<Done>> createLogicAndMaterializedValue(Attributes inheritedAttributes) throws Exception {
            CompletableFuture<Done> promise = new CompletableFuture<>();

            GraphStageLogic logic = new AbstractStreamingGeoJsonGraphStage(shape, featureConsumer) {

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

                                feeder.feedInput(bytes, 0, bytes.length);
                                boolean feedMeMore = false;
                                while (!feedMeMore) {
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
                                feeder.endOfInput();
                                //while (parser.hasNext())
                                //    advanceParser();
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
