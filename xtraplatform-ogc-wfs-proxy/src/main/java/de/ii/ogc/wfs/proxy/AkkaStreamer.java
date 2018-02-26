package de.ii.ogc.wfs.proxy;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.japi.Pair;
import akka.japi.function.Function2;
import akka.stream.ActorMaterializer;
import akka.stream.Attributes;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import de.ii.ogc.wfs.proxy.StreamingFeatureTransformer.TransformEvent;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import org.apache.felix.ipojo.annotations.*;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;
import scala.util.Try;

import javax.ws.rs.core.StreamingOutput;
import javax.xml.namespace.QName;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {AkkaStreamer.class})
@Instantiate
public class AkkaStreamer extends AllDirectives {
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(AkkaStreamer.class);

    @Controller
    private boolean ready;

    @Requires
    private BundleActorSystem bundleActorSystem;

    private CompletionStage<ServerBinding> binding;
    private Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, NotUsed> pool;
    private Flow<ByteString, TransformEvent, NotUsed> parser;
    private Flow<TransformEvent, ByteString, Consumer<OutputStream>> writer;

    private ActorSystem system;
    private Http http;
    private Materializer materializer;
    private ConnectHttp connectHttp;
    private Flow<HttpRequest, HttpResponse, NotUsed> routeFlow;

    private WFSRequest wfsRequest;

    public AkkaStreamer() {
        //this.system = bundleActorSystem.getSystem();
    }

    @Validate
    private void onStart() {
        LOGGER.getLogger().debug("HTTP STARTING");
        try {
            this.system = bundleActorSystem.getSystem();
            this.http = Http.get(system);
            this.materializer = ActorMaterializer.create(system);
            this.connectHttp = ConnectHttp.toHost("localhost", 7082);

            // start with empty route
            this.routeFlow = emptyRoute().flow(system, materializer);
            this.binding = startHttpServer();

            this.pool = http.superPool();
            this.ready = true;
        } catch (Throwable e) {
            LOGGER.getLogger().debug("HTTP START FAILED", e);
        }
    }

    @Invalidate
    private void onStop() {
        LOGGER.getLogger().debug("HTTP STOPPING");
        try {
            stopHttpServer();
        } catch (Throwable e) {
            LOGGER.getLogger().debug("HTTP STOP FAILED", e);
        }
    }

    private CompletionStage<ServerBinding> startHttpServer() {
        return http.bindAndHandle(routeFlow, connectHttp, materializer).thenApply(serverBinding -> {
            LOGGER.getLogger().debug("HTTP STARTED");
            return serverBinding;
        }).exceptionally(throwable -> {
            LOGGER.getLogger().debug("HTTP BIND FAILED", throwable);
            return null;
        });
    }

    private CompletionStage<Void> stopHttpServer() {
        if (binding != null) {
            return binding
                    .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                    .thenAccept(unbound -> LOGGER.getLogger().debug("HTTP STOPPED"));
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

    private Route createRoute() {
        return route(
                path("async", () ->
                        route(
                                get(() ->
                                        parameterOptional("page", page ->
                                                parameterOptional("count", count ->
                                                        complete((HttpEntity.Chunked)HttpEntities.createChunked(ContentTypes.APPLICATION_JSON, stream(page, count)).withoutSizeLimit())
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

    private Source<ByteString, NotUsed> stream(Optional<String> page, Optional<String> count) {
        String requestParams = page.map(p -> "&startIndex=" + p + "&").orElse("") + count.map(c -> "count=" + c).orElse("");
        LOGGER.getLogger().debug("HTTP REQUEST {}", requestParams);

        LoggingAdapter adapter = Logging.getLogger(system, "customLogger");
        adapter.debug("AKKA LOGGING");

        /*Source<ByteString, OutputStream> chunker = StreamConverters.asOutputStream(FiniteDuration.apply(5, TimeUnit.SECONDS)).log("OutputStream", adapter).withAttributes(Attributes.logLevels(Logging.DebugLevel(), Logging.DebugLevel(), Logging.DebugLevel()));


        Flow<TransformEvent, ByteString, NotUsed> transformer = Flow.fromSinkAndSourceCoupledMat(writer, chunker, (Function2<Consumer<OutputStream>, OutputStream, NotUsed>) (arg1, arg2) -> {
            //Futures.future(() -> {
            LOGGER.getLogger().debug("COUPLE");
            arg1.accept(arg2);
            //return null;
            //}, system.dispatcher());

            return NotUsed.getInstance();
        });*/



        Source<ByteString, NotUsed> stream =
                //Source.fromCompletionStage(http.singleRequest(HttpRequest.create(wfsRequest.getAsUrl())))
                Source.single(Pair.create(HttpRequest.create(wfsRequest.getAsUrl() + requestParams), null))
                        .via(pool)
                        .map(param -> {
                            //LOGGER.getLogger().debug("HTTP RESPONSE {}", param.toString());
                            return param.first().get();
                        })
                        .flatMapConcat(httpResponse -> {
                            LOGGER.getLogger().debug("HTTP RESPONSE {}", httpResponse.status());
                            return httpResponse.entity().withoutSizeLimit().getDataBytes();
                        })
                        .via(parser)//.map(t -> ByteString.fromString(t.toString() + "\\n"));
                        //.via(transformer);
                        .via(writer);

        Source<ByteString, NotUsed> byteStringNotUsedSource = Source.single(ByteString.fromString("{\"text\": \"HELLO WORLD\"}"));

        return stream;
    }

    public void stream(final WfsProxyFeatureType featureType, final WFSRequest wfsRequest, final String outputFormat, final boolean isFeatureCollection, final Flow<TransformEvent, ByteString, Consumer<OutputStream>> writer, final Consumer<StreamingOutput> onSuccess, final Function<Throwable, Void> onFailure) {
        //ActorSystem system = bundleActorSystem.getSystem();
        //final Http http = Http.get(system);
        //final Materializer materializer = ActorMaterializer.create(system);
        this.parser = StreamingFeatureTransformer.parser(new QName(featureType.getNamespace(), featureType.getName()), featureType.getMappings(), outputFormat);
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
