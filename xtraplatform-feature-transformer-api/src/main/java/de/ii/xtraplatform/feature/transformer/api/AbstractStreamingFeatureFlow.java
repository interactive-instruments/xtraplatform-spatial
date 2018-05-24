/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import akka.stream.*;
import akka.stream.javadsl.Source;
import akka.stream.stage.*;
import akka.util.ByteString;
import de.ii.xtraplatform.feature.transformer.api.EventBasedStreamingFeatureTransformer.TransformEvent;
import scala.Function1;
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
