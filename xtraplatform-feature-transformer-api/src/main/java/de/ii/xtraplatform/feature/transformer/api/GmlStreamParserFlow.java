/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import akka.Done;
import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.javadsl.Flow;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStageLogic;
import akka.stream.stage.GraphStageWithMaterializedValue;
import akka.util.ByteString;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureTransformer;
import scala.Tuple2;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
public class GmlStreamParserFlow {

    public static Flow<ByteString, ByteString, CompletionStage<Done>> consume(final QName featureType, final GmlConsumerFlow gmlConsumer) {
        return Flow.fromGraph(new GmlFlow(featureType, gmlConsumer));
    }

    public static Flow<ByteString, ByteString, CompletionStage<Done>> transform(final QName featureType, final FeatureTypeMapping featureTypeMapping, final GmlTransformerFlow gmlTransformer) {
        return GmlStreamParserFlow.consume(featureType, new FeatureTransformerFromGmlFlow(featureTypeMapping, gmlTransformer));
    }

    public interface GmlTransformerFlow extends FeatureTransformer {
        void initialize(Consumer<ByteString> push, Consumer<Throwable> failStage);
    }

    static class FeatureTransformerFromGmlFlow extends FeatureTransformerFromGml implements GmlConsumerFlow {

        FeatureTransformerFromGmlFlow(FeatureTypeMapping featureTypeMapping, final GmlTransformerFlow gmlTransformer) {
            super(featureTypeMapping, gmlTransformer, ImmutableList.of(), ImmutableMap.of());
        }

        @Override
        public void initialize(Consumer<ByteString> push, Consumer<Throwable> failStage) {
            ((GmlTransformerFlow) featureTransformer).initialize(push, failStage);
        }
    }

    public interface GmlConsumerFlow extends FeatureConsumer {
        void initialize(Consumer<ByteString> push, Consumer<Throwable> failStage);
    }

    static class GmlFlow extends GraphStageWithMaterializedValue<FlowShape<ByteString, ByteString>, CompletionStage<Done>> {

        public final Inlet<ByteString> in = Inlet.create("GmlFlow.in");
        public final Outlet<ByteString> out = Outlet.create("GmlFlow.out");
        private final FlowShape<ByteString, ByteString> shape = FlowShape.of(in, out);

        private final QName featureType;
        private final GmlConsumerFlow gmlConsumer;
        private boolean pushed;

        GmlFlow(QName featureType, GmlConsumerFlow gmlConsumer) {
            this.featureType = featureType;
            this.gmlConsumer = gmlConsumer;
        }

        @Override
        public FlowShape<ByteString, ByteString> shape() {
            return shape;
        }

        @Override
        public Tuple2<GraphStageLogic, CompletionStage<Done>> createLogicAndMaterializedValue(Attributes inheritedAttributes) throws XMLStreamException {
            CompletableFuture<Done> promise = new CompletableFuture<>();

            GraphStageLogic logic =  new AbstractStreamingGmlGraphStage(shape, ImmutableList.of(featureType), gmlConsumer) {

                boolean started = false;

                @Override
                public void preStart() throws Exception {
                    super.preStart();
                    System.out.println("preStart");
                    gmlConsumer.initialize(byteString -> {
                        push(out, byteString);
                        pushed = true;
                    }, this::failStage);
                }

                {
                    setHandler(in, new AbstractInHandler() {
                        @Override
                        public void onPush() throws Exception {
                            byte[] bytes = grab(in).toArray();
                            parser.getInputFeeder().feedInput(bytes, 0, bytes.length);
                            boolean feedMeMore = false;
                            while (!pushed && !feedMeMore && parser.hasNext()) {
                                boolean closed = isClosed(in);
                                if (closed) break;
                                feedMeMore = advanceParser();
                            }
                            if (feedMeMore && !isClosed(in)) {
                                pull(in);
                            }
                        }

                        @Override
                        public void onUpstreamFinish() throws Exception {
                            parser.getInputFeeder().endOfInput();
                            while (isAvailable(out) && parser.hasNext())
                                advanceParser();

                            completeStage();
                        }

                        @Override
                        public void onUpstreamFailure(Throwable ex) throws Exception {
                            super.onUpstreamFailure(ex);
                        }
                    });

                    setHandler(out, new AbstractOutHandler() {
                        @Override
                        public void onPull() throws Exception {
                            pushed = false;
                            boolean feedMeMore = false;
                            while (!pushed && !feedMeMore && started && parser.hasNext()) {
                                feedMeMore = advanceParser();
                            }
                            if (!pushed || feedMeMore) {
                                pull(in);
                                started = true;
                            }
                        }

                        @Override
                        public void onDownstreamFinish() throws Exception {
                            super.onDownstreamFinish();
                        }
                    });
                }
            };

            return new Tuple2<>(logic, promise);
        }

    }
}
