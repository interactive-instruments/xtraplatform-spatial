/**
 * Copyright 2019 interactive instruments GmbH
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.features.domain.FeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import javax.xml.namespace.QName;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zahnen
 */
public class GmlStreamParser {

    public static Sink<ByteString, CompletionStage<Done>> consume(final QName featureType,
                                                                  final FeatureConsumer gmlConsumer) {
        return consume(ImmutableList.of(featureType), gmlConsumer);
    }

    public static Sink<ByteString, CompletionStage<Done>> consume(final List<QName> featureTypes,
                                                                  final FeatureConsumer gmlConsumer) {
        return Sink.fromGraph(new FeatureSinkFromGml(featureTypes, gmlConsumer));
    }

    public static Sink<ByteString, CompletionStage<Done>> transform(final QName featureTypeName,
                                                                    final FeatureType featureType,
                                                                    final FeatureTransformer2 featureTransformer,
                                                                    List<String> fields) {
        return transform(featureTypeName, featureType, featureTransformer, fields, ImmutableMap.of());
    }

    public static Sink<ByteString, CompletionStage<Done>> transform(final QName featureTypeName,
                                                                    final FeatureType featureType,
                                                                    final FeatureTransformer2 featureTransformer,
                                                                    List<String> fields,
                                                                    Map<QName, List<String>> resolvableTypes) {
        List<QName> featureTypes = resolvableTypes.isEmpty() ? ImmutableList.of(featureTypeName) : ImmutableList.<QName>builder().add(featureTypeName)
                                                                                                                             .addAll(resolvableTypes.keySet())
                                                                                                                             .build();
        return GmlStreamParser.consume(featureTypes, new FeatureTransformerFromGml2(featureType, featureTransformer, fields, resolvableTypes));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GmlStreamParser.class);

    static class FeatureSinkFromGml extends GraphStageWithMaterializedValue<SinkShape<ByteString>, CompletionStage<Done>> {

        public final Inlet<ByteString> in = Inlet.create("FeatureSinkFromGml.in");
        private final SinkShape<ByteString> shape = SinkShape.of(in);

        private final List<QName> featureTypes;
        private final FeatureConsumer gmlConsumer;
        private String bufferOpening = null;
        private String bufferMembers = null;
        private String bufferAdditional = null;

        FeatureSinkFromGml(List<QName> featureTypes, FeatureConsumer gmlConsumer) {
            this.featureTypes = featureTypes;
            this.gmlConsumer = gmlConsumer;
        }

        @Override
        public SinkShape<ByteString> shape() {
            return shape;
        }

        @Override
        public Tuple2<GraphStageLogic, CompletionStage<Done>> createLogicAndMaterializedValue(
                Attributes inheritedAttributes) throws Exception {
            CompletableFuture<Done> promise = new CompletableFuture<>();

            GraphStageLogic logic = new AbstractStreamingGmlGraphStage(shape, featureTypes, gmlConsumer) {
                int chunks = 0;

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

                                //TODO more than one chunk
                                if (featureTypes.size() > 1) {
                                    String s = new String(bytes, StandardCharsets.UTF_8);

                                    int firstMember = -1;
                                    Matcher matcher = Pattern.compile("<(?:\\w+:)?member>")
                                                             .matcher(s);
                                    if (matcher.find()) {
                                        firstMember = matcher.start();
                                    }

                                    int additionalObjectsStart = -1;
                                    Matcher matcher2 = Pattern.compile("<(?:\\w+:)?additionalObjects>")
                                                              .matcher(s);
                                    if (matcher2.find()) {
                                        additionalObjectsStart = matcher2.start();
                                    }

                                    int additionalObjectsEnd = -1;
                                    Matcher matcher3 = Pattern.compile("</(?:\\w+:)?additionalObjects>")
                                                              .matcher(s);
                                    if (matcher3.find()) {
                                        additionalObjectsEnd = matcher3.end();
                                    }

                                    //opening tag
                                    if (firstMember > 0 && Objects.isNull(bufferOpening)) {
                                        bufferOpening = s.substring(0, firstMember);
                                        bufferMembers = s.substring(firstMember, additionalObjectsStart > -1 ? additionalObjectsStart : s.length());
                                    }
                                    //members
                                    else if (Objects.nonNull(bufferMembers) && Objects.isNull(bufferAdditional)) {
                                        bufferMembers += s.substring(0, additionalObjectsStart > -1 ? additionalObjectsStart : s.length());
                                    }

                                    //additional objects
                                    if (additionalObjectsStart > -1) {
                                        bufferAdditional = s.substring(additionalObjectsStart, additionalObjectsEnd > -1 ? additionalObjectsEnd : s.length());
                                    } else if (Objects.nonNull(bufferAdditional)) {
                                        bufferAdditional += s.substring(0, additionalObjectsEnd > -1 ? additionalObjectsEnd : s.length());
                                    }

                                    //closing tag
                                    if (additionalObjectsEnd > 0) {
                                        String rest = s.substring(additionalObjectsEnd + 1);
                                        bytes = (bufferOpening + bufferAdditional + bufferMembers + rest).getBytes();
                                    }
                                    //get next chunk
                                    else if (!isClosed(in)) {
                                        pull(in);
                                        return;
                                    }
                                }

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
                                parser.getInputFeeder()
                                      .endOfInput();
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
