package de.ii.xtraplatform.ogc.api.gml.parser;

import akka.NotUsed;
import akka.stream.*;
import akka.stream.javadsl.Flow;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.util.ByteString;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
public class StreamingGmlParserFlow {

    public static Flow<ByteString, ByteString, NotUsed> parser(final QName featureType, final GmlConsumerFlow gmlConsumer) {
        return Flow.fromGraph(new GmlFlow(featureType, gmlConsumer));
    }

    public interface GmlConsumerFlow extends StreamingGmlParserSink.GmlConsumer {
        void initialize(Consumer<ByteString> push, Consumer<Throwable> failStage);
    }

    static class GmlFlow extends GraphStage<FlowShape<ByteString, ByteString>> {

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
        public GraphStageLogic createLogic(Attributes inheritedAttributes) throws XMLStreamException {
            return new AbstractStreamingGmlGraphStage(shape, featureType, gmlConsumer) {

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
                                feedMeMore = advanceParser(() -> {
                                    if (isClosed(in)) return false;
                                    else if (!pushed) pull(in);
                                    return true;
                                });
                            }
                            if (feedMeMore && !isClosed(in)) {
                                pull(in);
                            }
                        }

                        @Override
                        public void onUpstreamFinish() throws Exception {
                            parser.getInputFeeder().endOfInput();
                            while (isAvailable(out) && parser.hasNext())
                                advanceParser(() -> {
                                    if (isClosed(in)) return false;
                                    else {
                                        pull(in);
                                        return true;
                                    }
                                });

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
                                feedMeMore = advanceParser(() -> {
                                    if (isClosed(in)) return false;
                                    else {
                                        pull(in);
                                        return true;
                                    }
                                });
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
        }

    }
}
