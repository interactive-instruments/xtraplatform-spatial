package de.ii.ogc.wfs.proxy;

import akka.stream.*;
import akka.stream.javadsl.Source;
import akka.stream.stage.*;
import akka.util.ByteString;
import de.ii.ogc.wfs.proxy.StreamingFeatureTransformer.TransformEvent;
import scala.Function1;
import scala.PartialFunction;
import scala.Tuple2;
import scala.runtime.AbstractPartialFunction;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author zahnen
 */
public abstract class AbstractStreamingFeatureFlow extends GraphStageWithMaterializedValue<FlowShape<TransformEvent, ByteString>, StreamingOutput> {

    private final Inlet<TransformEvent> in = Inlet.create("AbstractStreamingFeatureWriter.in");
    private final Outlet<ByteString> out = Outlet.create("AbstractStreamingFeatureWriter.out");
    private final FlowShape<TransformEvent, ByteString> shape = FlowShape.of(in, out);

    @Override
    public FlowShape<TransformEvent, ByteString> shape() {
        return shape;
    }

    protected abstract void writeEvent(final TransformEvent transformEvent) throws IOException;
    protected abstract StreamingOutput getStreamingOutput();
    protected abstract Source<ByteString, OutputStream> getSource();

    @Override
    public Tuple2<GraphStageLogic, StreamingOutput> createLogicAndMaterializedValue(Attributes inheritedAttributes) throws Exception {
        GraphStageLogic graphStageLogic = new GraphStageLogic(shape) {

            // state

            {
                setHandler(in, new AbstractInHandler() {

                    @Override
                    public void onPush() throws Exception {
                        final TransformEvent transformEvent = grab(in);


                        writeEvent(transformEvent);
                        getSource().collect(new AbstractPartialFunction<ByteString, Object>() {
                            @Override
                            public boolean isDefinedAt(ByteString x) {
                                return false;
                            }

                            @Override
                            public <U> Function1<ByteString, Object> runWith(Function1<Object, U> action) {
                                return super.runWith(action);
                            }
                        });

                        pull(in);
                    }
                });

                setHandler(out, new AbstractOutHandler() {
                    @Override
                    public void onPull() throws Exception {
                        pull(in);
                    }
                });
            }
        };

        return Tuple2.apply(graphStageLogic, getStreamingOutput());
    }

    //@Override
    //public GraphStageLogic createLogic(Attributes inheritedAttributes) throws Exception {
    //    return
    //}
}
