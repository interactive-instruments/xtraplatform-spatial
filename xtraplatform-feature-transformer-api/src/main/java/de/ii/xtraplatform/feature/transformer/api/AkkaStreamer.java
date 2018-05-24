/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.coding.Coder;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.headers.AcceptEncoding;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.settings.ConnectionPoolSettings;
import akka.http.scaladsl.model.headers.HttpEncodings;
import akka.japi.Pair;
import akka.japi.function.Function2;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import de.ii.xtraplatform.akka.http.ActorSystemProvider;
import de.ii.xtraplatform.akka.http.AkkaHttp;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Controller;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

import javax.ws.rs.core.StreamingOutput;
import javax.xml.namespace.QName;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {AkkaStreamer.class})
//@Instantiate
public class AkkaStreamer extends AllDirectives {
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaStreamer.class);

    @Controller
    private boolean ready;

    @Requires
    private ActorSystemProvider actorSystemProvider;

    @Requires
    private AkkaHttp akkaHttp;

    private CompletionStage<ServerBinding> binding;
    private Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, NotUsed> pool;
    private Supplier<Flow<ByteString, EventBasedStreamingFeatureTransformer.TransformEvent, NotUsed>> parser;
    private Supplier<Flow<ByteString, ByteString, CompletionStage<Done>>> writer;

    private ActorSystem system;
    private Http http;
    private Materializer materializer;
    private ConnectHttp connectHttp;
    private Flow<HttpRequest, HttpResponse, NotUsed> routeFlow;

    private Optional<String> wfsRequest;

    public AkkaStreamer() {
        //this.system = bundleActorSystem.getSystem();
    }

    @Validate
    private void onStart() {
        LOGGER.debug("HTTP STARTING");
        try {
            this.system = actorSystemProvider.getActorSystem(null, null);
            this.http = Http.get(system);
            this.materializer = ActorMaterializer.create(system);
            this.connectHttp = ConnectHttp.toHost("localhost", 7082);

            // start with empty route
            this.routeFlow = emptyRoute().flow(system, materializer);
            this.binding = startHttpServer();

            this.pool = http.superPool(ConnectionPoolSettings.create(system).withMaxConnections(64), system.log());
            this.ready = true;
        } catch (Throwable e) {
            LOGGER.debug("HTTP START FAILED", e);
        }
    }

    @Invalidate
    private void onStop() {
        LOGGER.debug("HTTP STOPPING");
        try {
            stopHttpServer();
        } catch (Throwable e) {
            LOGGER.debug("HTTP STOP FAILED", e);
        }
    }

    private CompletionStage<ServerBinding> startHttpServer() {
        return http.bindAndHandle(routeFlow, connectHttp, materializer).thenApply(serverBinding -> {
            LOGGER.debug("HTTP STARTED");
            return serverBinding;
        }).exceptionally(throwable -> {
            LOGGER.debug("HTTP BIND FAILED", throwable);
            return null;
        });
    }

    private CompletionStage<Void> stopHttpServer() {
        if (binding != null) {
            return binding
                    .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                    .thenAccept(unbound -> LOGGER.debug("HTTP STOPPED"));
        }
        return CompletableFuture.allOf();
    }

    private CompletionStage<ServerBinding> restartHttpServer() {
        return stopHttpServer().thenCompose(unbound -> startHttpServer());
    }

    public void addRoute() {
        this.routeFlow = createRoute().flow(system, materializer);

        this.binding = restartHttpServer();
    }

    // TODO: jax-rs to akka-http (https://jersey.java.net/apidocs/2.16/jersey/org/glassfish/jersey/server/model/Resource.html)
    private Route createRoute() {
        return route(
                path("async", () ->
                        route(
                                get(() ->
                                        parameterOptional("page", page ->
                                                parameterOptional("count", count ->
                                                        complete((HttpEntity.Chunked) HttpEntities.createChunked(ContentTypes.TEXT_PLAIN_UTF8, stream(page, count)).withoutSizeLimit())
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private Route emptyRoute() {
        return route(
                pathEndOrSingleSlash(() -> route(
                        get(() -> complete("EMPTY"))
                ))
        );
    }

    private Source<ByteString, Date> stream(Optional<String> page, Optional<String> count) {
        return stream(page, count, wfsRequest, writer);
    }

    private Source<ByteString, Date> stream(Optional<String> page, Optional<String> count, Optional<String> wfsRequest, Supplier<Flow<ByteString, ByteString, CompletionStage<Done>>> writer) {
        String requestParams = page.map(p -> "&startIndex=" + p + "&").orElse("") + count.map(c -> "count=" + c).orElse("");
        LOGGER.debug("HTTP REQUEST {}", requestParams);

        LoggingAdapter adapter = Logging.getLogger(system, "customLogger");
        adapter.debug("AKKA LOGGING");

        /*Source<ByteString, OutputStream> chunker = StreamConverters.asOutputStream(FiniteDuration.apply(5, TimeUnit.SECONDS)).log("OutputStream", adapter).withAttributes(Attributes.logLevels(Logging.DebugLevel(), Logging.DebugLevel(), Logging.DebugLevel()));


        Flow<TransformEvent, ByteString, NotUsed> transformer = Flow.fromSinkAndSourceCoupledMat(writer, chunker, (Function2<Consumer<OutputStream>, OutputStream, NotUsed>) (arg1, arg2) -> {
            //Futures.future(() -> {
            LOGGER.debug("COUPLE");
            arg1.accept(arg2);
            //return null;
            //}, system.dispatcher());

            return NotUsed.getInstance();
        });*/

        final Function<HttpResponse, HttpResponse> decodeResponse = response -> {
            // Pick the right coder
            final Coder coder;
            if (HttpEncodings.gzip().equals(response.encoding())) {
                coder = Coder.Gzip;
            } else if (HttpEncodings.deflate().equals(response.encoding())) {
                coder = Coder.Deflate;
            } else {
                coder = Coder.NoCoding;
            }
            LOGGER.debug("HTTP Encoding {}", coder);

            // Decode the entity
            return coder.decodeMessage(response);
        };


        // TODO: measure performance with files to compare processing time only
//        Source<ByteString, Date> fromFile = FileIO.fromFile(new File("/home/zahnen/development/ldproxy/artillery/flurstueck-" + count.get() + "-" + page.get() + ".xml"))
//                .mapMaterializedValue(nu -> new Date());

        Source<ByteString, Date> fromWfs =
                //Source.fromCompletionStage(http.singleRequest(HttpRequest.create(wfsRequest.getAsUrl())))
                Source.single(Pair.create(HttpRequest.create(wfsRequest.get() + requestParams).addHeader(AcceptEncoding.create(HttpEncodings.deflate().toRange(), HttpEncodings.gzip().toRange(), HttpEncodings.chunked().toRange())), null))
                        .via(pool)
                        .map(param -> {
                            //LOGGER.debug("HTTP RESPONSE {}", param.toString());
                            return param.first().get();
                        })
                        .map(decodeResponse::apply)
                        .mapMaterializedValue(nu -> new Date())
                        .flatMapConcat(httpResponse -> {
                            LOGGER.debug("HTTP RESPONSE {}", httpResponse.status());
                            return httpResponse.entity().withoutSizeLimit().getDataBytes();
                        });

        Flow<ByteString, ByteString, NotUsed> mergeBuffer = Flow.of(ByteString.class)
                .conflate((Function2<ByteString, ByteString, ByteString>) ByteString::concat);

        // TODO: load tests with throttled file stream, simulate wfs with e.g. 4 cores and a certain throughput
        Source<ByteString, Date> stream =
                //fromFile
                fromWfs
                        //.buffer(128, OverflowStrategy.backpressure())
                        //.via(parser.get())
                        //.watchTermination((start, isDone) -> isDone.thenRun(() -> LOGGER.debug("TOOK: {}", new Date().getTime() - start.getTime())))
                        //.map(t -> ByteString.fromString(/*t.toString() +*/ "."))
                        //.buffer(32768, OverflowStrategy.backpressure())
                        //.via(transformer);

                        .via(writer.get())
                        //.via(mergeBuffer)
                ;

        Source<ByteString, NotUsed> byteStringNotUsedSource = Source.single(ByteString.fromString("{\"text\": \"HELLO WORLD\"}"));

        return stream;
    }

    public void streamFeatures(final FeatureTypeConfiguration featureType, final Optional<String> wfsRequest, final String outputFormat, final boolean isFeatureCollection, final Supplier<Flow<ByteString, ByteString, CompletionStage<Done>>> writer, final Consumer<StreamingOutput> onSuccess, final Function<Throwable, Void> onFailure) {
        //ActorSystem system = bundleActorSystem.getSystem();
        //final Http http = Http.get(system);
        //final Materializer materializer = ActorMaterializer.create(system);
        Source<ByteString, Date> stream = stream(Optional.empty(), Optional.empty(), wfsRequest, writer);



        //InputStream inputStream = stream.runWith(StreamConverters.asInputStream(), materializer);
        //List<RunnableGraph<CompletionStage<Done>>> to = new ArrayList<>();
        onSuccess.accept(outputStream -> {
            CompletionStage<Done> doneCompletionStage = stream.runWith(Sink.foreach(byteString -> {
                outputStream.write(byteString.toArray());
                outputStream.flush();
            }), materializer);

            doneCompletionStage.toCompletableFuture().join();
            outputStream.close();
        });

        //to.get(0).run(materializer);
    }

    public void stream(final FeatureTypeConfiguration featureType, final Optional<String> wfsRequest, final String outputFormat, final boolean isFeatureCollection, final Supplier<Flow<ByteString, ByteString, CompletionStage<Done>>> writer, final Consumer<StreamingOutput> onSuccess, final Function<Throwable, Void> onFailure) {
        //ActorSystem system = bundleActorSystem.getSystem();
        //final Http http = Http.get(system);
        //final Materializer materializer = ActorMaterializer.create(system);
        this.parser = () -> EventBasedStreamingFeatureTransformer.parser(new QName(featureType.getNamespace(), featureType.getName()), featureType.getMappings(), outputFormat);
        this.writer = writer;
        this.wfsRequest = wfsRequest;
        addRoute();

        onSuccess.accept(outputStream -> {
            outputStream.write("DONE".getBytes());
            outputStream.close();
        });

        //ActorRef requestActor = system.actorOf(SingleRequestInActorExample.props());
        //ActorRef writeActor = system.actorOf(FeatureWriterActor.props(new QName(featureType.getNamespace(), featureType.getName()), featureType.getMappings(), outputFormat, writer));

        /*Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, NotUsed> pool = http.superPool();

        Source.single(Pair.create(HttpRequest.create(wfsRequest.getAsUrl()), null))
                .via(pool)
                .map(param -> param.first().get())
                .flatMapConcat(httpResponse -> httpResponse.entity().getDataBytes())
                .via(parser)
                .toMat(writer.apply(null), (arg1, arg2) -> {
                    onSuccess.accept(arg2);
                    return arg2;
                })
                .run(materializer);
*/
                /*.map(httpResponse ->
                        (StreamingOutput) outputStream ->
                                httpResponse.entity().getDataBytes()
                                        .via(parser)
                                        .runWith(writer.apply(outputStream), materializer)
                )
                .thenAccept(onSuccess)
                .exceptionally(onFailure);*/

        //final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(30, TimeUnit.SECONDS));
        //PatternsCS.ask(requestActor, wfsRequest.getAsUrl(), timeout);
        //requestActor.tell(wfsRequest.getAsUrl(), writeActor);
    }

    /*static class FeatureWriterActor extends AbstractActor {
        final Http http = Http.get(context().system());
        final ExecutionContextExecutor dispatcher = context().dispatcher();
        final Materializer materializer = ActorMaterializer.create(context());
        final LoggingAdapter log = context().system().log();

        final Flow<ByteString, TransformEvent, NotUsed> parser;
        final Sink<TransformEvent, NotUsed> writer;

        FeatureWriterActor(final QName featureTypeName, final WfsProxyFeatureTypeMapping featureTypeMapping, final String outputFormat, final Sink<TransformEvent, NotUsed> writer) throws IOException {
            this.parser = StreamingFeatureTransformer.parser(featureTypeName, featureTypeMapping, outputFormat);
            this.writer = writer;//GeoJsonFeatureWriter.writer(jsonGenerator);
        }

        public static Props props(final QName featureTypeName, final WfsProxyFeatureTypeMapping featureTypeMapping, final String outputFormat, final Sink<TransformEvent, NotUsed> writer) {
            return Props.create(FeatureWriterActor.class, featureTypeName, featureTypeMapping, outputFormat, writer);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, url ->
                    //PatternsCS.pipe(fetch(url), dispatcher).to(self()))
                    //.match(HttpResponse.class, httpResponse ->
    {
                        //log.info("RESPONSE: " + httpResponse.status());

                        CompletionStage<NotUsed> notUsedCompletionStage = fetch(url).thenApply(httpResponse -> httpResponse.entity().getDataBytes()
                                //httpResponse.entity().getDataBytes()
                                .via(parser)
                                .runWith(writer, materializer)
                        );
                        //.runWith(Sink.foreach(event -> log.info(event.toString())), materializer);
                    })
                    .build();
        }

        CompletionStage<HttpResponse> fetch(String url) {
            return http.singleRequest(HttpRequest.create(url));
        }
    }

    static class SingleRequestInActorExample extends AbstractActor {
        final Http http = Http.get(context().system());
        final ExecutionContextExecutor dispatcher = context().dispatcher();
        final Materializer materializer = ActorMaterializer.create(context());
        final LoggingAdapter log = context().system().log();

        public static Props props() {
            return Props.create(SingleRequestInActorExample.class);
        }

        @Override
        public AbstractActor.Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, url -> PatternsCS.pipe(fetch(url), dispatcher).to(getSender()))
                    .build();
        }

        CompletionStage<HttpResponse> fetch(String url) {
            return http.singleRequest(HttpRequest.create(url));
        }
    }*/
}
