/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.geojson;

import akka.Done;
import akka.NotUsed;
import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.SinkShape;
import akka.stream.javadsl.Flow;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.stream.stage.GraphStageWithMaterializedValue;
import akka.util.ByteString;
import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureProcessor;
import de.ii.xtraplatform.features.domain.FeatureReaderGeneric;
import de.ii.xtraplatform.features.domain.FeatureSchemaMapper;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * @author zahnen
 */
public class GeoJsonParserReactive {
/*
    public static Sink<ByteString, CompletionStage<Done>> sink(final FeatureReaderGeneric featureReader) {
        return Sink.fromGraph(new FeatureSinkFromGeoJson(featureReader));
    }

    public static <V extends PropertyBase<V>, W extends FeatureBase<V>> Sink<ByteString, CompletionStage<Done>> sinkOf(
            final FeatureTypeV2 featureType, final FeatureProcessor<V, W> featureProcessor) {
        return GeoJsonParserReactive.sink(new FeatureSchemaMapper(featureType, new FeatureReaderToProcessor<>(featureProcessor)));
    }
*/
    public static <U extends SchemaBase<U>, V extends PropertyBase<V,U>, W extends FeatureBase<V,U>> Flow<ByteString, W, NotUsed> flowOf(
            final SchemaMapping<U> mapping, final Supplier<W> featureCreator, final Supplier<V> propertyCreator) {

        return Flow.fromGraph(new FeatureFlowFromGeoJson<>(mapping, featureCreator, propertyCreator));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonParserReactive.class);

    static class FeatureSinkFromGeoJson extends GraphStageWithMaterializedValue<SinkShape<ByteString>, CompletionStage<Done>> {

        public final Inlet<ByteString> in = Inlet.create("FeatureSinkFromGeoJson.in");
        private final SinkShape<ByteString> shape = SinkShape.of(in);

        private final FeatureReaderGeneric featureReader;

        FeatureSinkFromGeoJson(FeatureReaderGeneric featureReader) {
            this.featureReader = featureReader;
        }

        @Override
        public SinkShape<ByteString> shape() {
            return shape;
        }

        @Override
        public Tuple2<GraphStageLogic, CompletionStage<Done>> createLogicAndMaterializedValue(
                Attributes inheritedAttributes) throws Exception {
            CompletableFuture<Done> promise = new CompletableFuture<>();

            GraphStageLogic logic = new GraphStageLogic(shape) {

                private final GeoJsonParser parser = new GeoJsonParser(featureReader);

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

                                parser.feedInput(bytes, 0, bytes.length);
                                boolean feedMeMore = false;
                                while (!feedMeMore) {
                                    feedMeMore = parser.advanceParser();
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
                                parser.endOfInput();
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

    static final class FeatureFlowFromGeoJson<V extends PropertyBase<V,U>, W extends FeatureBase<V,U>, U extends SchemaBase<U>> extends GraphStage<FlowShape<ByteString, W>> {

        private final SchemaMapping<U> schemaMapping;
        private final Supplier<W> featureCreator;
        private final Supplier<V> propertyCreator;

        public FeatureFlowFromGeoJson(SchemaMapping<U> schemaMapping,
                                      Supplier<W> featureCreator, Supplier<V> propertyCreator) {
            this.schemaMapping = schemaMapping;
            this.featureCreator = featureCreator;
            this.propertyCreator = propertyCreator;
        }

        public final Inlet<ByteString> in = Inlet.create("FeatureFlowFromGeoJson.in");
        public final Outlet<W> out = Outlet.create("FeatureFlowFromGeoJson.out");

        private final FlowShape<ByteString, W> shape = FlowShape.of(in, out);

        @Override
        public FlowShape<ByteString, W> shape() {
            return shape;
        }

        public GraphStageLogic createLogic(Attributes inheritedAttributes) throws IOException {
            return new GraphStageLogic(shape) {

                private final GeoJsonParser parser = new GeoJsonParser(new FeatureSchemaMapper<>(schemaMapping, new FeatureReaderToProcessor<>(new FeatureProcessor<V, W, U>() {
                    @Override
                    public W createFeature() {
                        return featureCreator.get();
                    }

                    @Override
                    public V createProperty() {
                        return propertyCreator.get();
                    }

                    @Override
                    public void process(W feature) throws IOException {
                        push(out, feature);
                    }
                })));

                {
                    setHandler(
                            in,
                            new AbstractInHandler() {
                                @Override
                                public void onPush() throws Exception {
                                    byte[] bytes = grab(in).toArray();

                                    if (LOGGER.isTraceEnabled()) {
                                        LOGGER.trace(new String(bytes, StandardCharsets.UTF_8));
                                    }

                                    parser.feedInput(bytes, 0, bytes.length);
                                    boolean feedMeMore = false;
                                    while (!feedMeMore) {
                                        feedMeMore = parser.advanceParser();
                                    }
                                    if (feedMeMore && !isClosed(in)) {
                                        pull(in);
                                    }
                                }

                                @Override
                                public void onUpstreamFinish() throws Exception {
                                    parser.endOfInput();

                                    completeStage();
                                }
                            });

                    setHandler(
                            out,
                            new AbstractOutHandler() {
                                @Override
                                public void onPull() throws Exception {
                                    pull(in);
                                }
                            });
                }
            };
        }
    }
}
