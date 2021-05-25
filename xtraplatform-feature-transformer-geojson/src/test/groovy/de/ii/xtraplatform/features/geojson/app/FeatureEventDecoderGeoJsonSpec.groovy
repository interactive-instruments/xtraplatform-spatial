/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.geojson.app

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.javadsl.Flow
import akka.stream.javadsl.Source
import akka.testkit.javadsl.TestKit
import akka.util.ByteString
import com.typesafe.config.Config
import de.ii.xtraplatform.features.domain.FeatureEventConsumer
import de.ii.xtraplatform.features.domain.FeatureEventDecoder
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import de.ii.xtraplatform.streams.app.ReactiveAkka
import de.ii.xtraplatform.streams.domain.ActorSystemProvider
import de.ii.xtraplatform.streams.domain.Reactive
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.Consumer
import java.util.function.Function

/**
 * @author zahnen
 */
//@Ignore//TODO
class FeatureEventDecoderGeoJsonSpec extends Specification {

    static final Logger LOGGER = LoggerFactory.getLogger(FeatureEventDecoderGeoJsonSpec.class)

    @Shared
    ActorSystem system
    @Shared
    Reactive reactive
    @Shared
    Reactive.Runner runner

    FeatureEventConsumer eventConsumer
    FeatureEventConsumer loggingEventConsumer
    FeatureEventDecoder<byte[]> decoder

    def setupSpec() {
        reactive = new ReactiveAkka(null, new ActorSystemProvider() {
            @Override
            ActorSystem getActorSystem(BundleContext context) {
                return null
            }

            @Override
            ActorSystem getActorSystem(BundleContext context, Config config) {
                return null
            }

            @Override
            ActorSystem getActorSystem(BundleContext context, Config config, String name) {
                system = ActorSystem.create(name, config)
                return system
            }
        })
        runner = reactive.runner("test")
    }

    def cleanupSpec() {
        runner.close()
        TestKit.shutdownActorSystem(system)
        system = null
    }

    def setup() {
        eventConsumer = Mock()
        loggingEventConsumer = Mock({
            onStart(*_) >> {args -> printf("start %s %s\n", args)}
            onFeatureStart(*_) >> {args -> printf("start feature\n")}
            onObjectStart(*_) >> {args -> printf("start object %s %s\n", args)}
            onArrayStart(*_) >> {args -> printf("start array %s\n", args)}
            onValue(*_) >> {args -> printf("value %s %s %s\n", args)}
            onArrayEnd(*_) >> {args -> printf("end array\n")}
            onObjectEnd(*_) >> {args -> printf("end object\n")}
            onFeatureEnd(*_) >> {args -> printf("end feature\n")}
            onEnd(*_) >> {args -> printf("end\n")}
        })

        decoder = new FeatureEventDecoderGeoJson()
        decoder.fuse(new Reactive.TranformerCustomFuseableIn<Object, Object, FeatureEventConsumer>() {
            @Override
            FeatureEventConsumer fuseableSink() {
                return eventConsumer
            }
            @Override
            void init(Consumer<Object> push) {}
            @Override
            void onPush(Object o) {}
            @Override
            void onComplete() {}
        })
    }

    def runStream(Reactive.Stream<?> stream) {
        def result = stream.on(runner).run().toCompletableFuture().join()
        println(result.toString())
        return result
    }

    static Reactive.Source<byte[]> FileSource(String path) {Reactive.Source.akka(Source.from([ByteString.fromString(new File(path).text).toArray()]))}

    def 'single feature'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/simple.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart(*_)

        then:
        1 * eventConsumer.onFeatureStart()

        then:
        1 * eventConsumer.onValue(["id"], "24", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onValue(["kennung"], "611320001-1", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onFeatureEnd()

        then:
        1 * eventConsumer.onEnd()

    }

    def 'single feature with point geometry'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/simple_point.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart(*_)

        then:
        1 * eventConsumer.onFeatureStart()

        then:
        1 * eventConsumer.onValue(["id"], "24", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onObjectStart(["geometry"], Optional.of(SimpleFeatureGeometry.POINT))

        then:
        1 * eventConsumer.onArrayStart(["geometry"])

        then:
        1 * eventConsumer.onValue(["geometry"], "8.18523495507722", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "49.698295103021096", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onObjectEnd()

        then:
        1 * eventConsumer.onValue(["kennung"], "611320001-1", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onFeatureEnd()

        then:
        1 * eventConsumer.onEnd()

    }

    def 'single feature with multipoint geometry'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/simple_multipoint.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart(*_)

        then:
        1 * eventConsumer.onFeatureStart()

        then:
        1 * eventConsumer.onValue(["id"], "20", {})

        then:
        1 * eventConsumer.onObjectStart(["geometry"], Optional.of(SimpleFeatureGeometry.MULTI_POINT))

        then:
        1 * eventConsumer.onArrayStart(["geometry"])

        then:
        1 * eventConsumer.onValue(["geometry"], "6.406233970262905", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "50.1501333536934", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "7.406233970262905", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "51.1501333536934", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onObjectEnd()

        then:
        1 * eventConsumer.onValue(["kennung"], "580410003-1", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onFeatureEnd()

        then:
        1 * eventConsumer.onEnd()

    }

    def 'single feature with multipolygon geometry'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/simple_multipolygon.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart(*_)

        then:
        1 * eventConsumer.onFeatureStart()

        then:
        1 * eventConsumer.onValue(["id"], "21", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onObjectStart(["geometry"], Optional.of(SimpleFeatureGeometry.MULTI_POLYGON))

        then:
        1 * eventConsumer.onArrayStart(["geometry"])

        then:
        1 * eventConsumer.onArrayStart(["geometry"])

        then:
        1 * eventConsumer.onArrayStart(["geometry"])

        then:
        1 * eventConsumer.onValue(["geometry"], "8.18523495507722", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "49.698295103021096", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "8.185283687843047", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "49.69823291309017", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onArrayStart(["geometry"])

        then:
        1 * eventConsumer.onArrayStart(["geometry"])

        then:
        1 * eventConsumer.onValue(["geometry"], "8.185681115675656", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "49.698286680057166", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "8.185796151881165", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "49.69836248910692", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onArrayStart(["geometry"])

        then:
        1 * eventConsumer.onValue(["geometry"], "8.186313615874417", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "49.698603368350874", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "8.18641074595947", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "49.69866280390489", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onObjectEnd()

        then:
        1 * eventConsumer.onValue(["kennung"], "631510001-1", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onFeatureEnd()

        then:
        1 * eventConsumer.onEnd()

    }

    def 'single feature with nested object'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/nested_object.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart(*_)

        then:
        1 * eventConsumer.onFeatureStart()

        then:
        1 * eventConsumer.onValue(["id"], "24", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onObjectStart(["erfasser"], Optional.empty())

        then:
        1 * eventConsumer.onValue(["erfasser", "name"], "John Doe", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onObjectEnd()

        then:
        1 * eventConsumer.onValue(["kennung"], "611320001-1", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onFeatureEnd()

        then:
        1 * eventConsumer.onEnd()

    }

    def 'single feature with value array'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/array_value.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart(*_)

        then:
        1 * eventConsumer.onFeatureStart()

        then:
        1 * eventConsumer.onValue(["id"], "24", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onArrayStart(["erfasser_array"])

        then:
        1 * eventConsumer.onValue(["erfasser_array"], "John Doe", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onValue(["erfasser_array"], "Jane Doe", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onValue(["kennung"], "611320001-1", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onFeatureEnd()

        then:
        1 * eventConsumer.onEnd()

    }

    def 'single feature with nested object arrays and value arrays'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/nested_array_object.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart(*_)

        then:
        1 * eventConsumer.onFeatureStart()

        then:
        1 * eventConsumer.onValue(["id"], "24", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onArrayStart(["raumreferenz"])

        then:
        1 * eventConsumer.onObjectStart(["raumreferenz"], Optional.empty())

        then:
        1 * eventConsumer.onArrayStart(["raumreferenz", "ortsangabe"])

        then:
        1 * eventConsumer.onObjectStart(["raumreferenz", "ortsangabe"], Optional.empty())

        then:
        1 * eventConsumer.onValue(["raumreferenz", "ortsangabe", "kreisschluessel"], "11", SchemaBase.Type.INTEGER)

        then:
        1 * eventConsumer.onArrayStart(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"])

        then:
        1 * eventConsumer.onValue(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], "34", SchemaBase.Type.INTEGER)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onObjectEnd()

        then:
        1 * eventConsumer.onObjectStart(["raumreferenz", "ortsangabe"], Optional.empty())

        then:
        1 * eventConsumer.onArrayStart(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"])

        then:
        1 * eventConsumer.onValue(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], "35", SchemaBase.Type.INTEGER)

        then:
        1 * eventConsumer.onValue(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], "36", SchemaBase.Type.INTEGER)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onObjectEnd()

        then:
        1 * eventConsumer.onObjectStart(["raumreferenz", "ortsangabe"], Optional.empty())

        then:
        1 * eventConsumer.onValue(["raumreferenz", "ortsangabe", "kreisschluessel"], "12", SchemaBase.Type.INTEGER)

        then:
        1 * eventConsumer.onArrayStart(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"])

        then:
        1 * eventConsumer.onValue(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], "37", SchemaBase.Type.INTEGER)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onObjectEnd()

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onObjectEnd()

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onValue(["kennung"], "611320001-1", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onFeatureEnd()

        then:
        1 * eventConsumer.onEnd()

    }

    def 'collection with 3 features'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/collection.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart(OptionalLong.of(3), OptionalLong.of(12))

        then:
        1 * eventConsumer.onFeatureStart()

        then:
        1 * eventConsumer.onValue(["id"], "19", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onObjectStart(["geometry"], Optional.of(SimpleFeatureGeometry.POINT))

        then:
        1 * eventConsumer.onArrayStart(["geometry"])

        then:
        1 * eventConsumer.onValue(["geometry"], "6.295202392345018", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "50.11336914792363", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onObjectEnd()

        then:
        1 * eventConsumer.onValue(["kennung"], "580340001-1", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onFeatureEnd()

        then:
        1 * eventConsumer.onFeatureStart()

        then:
        1 * eventConsumer.onValue(["id"], "20", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onValue(["kennung"], "580410003-1", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onFeatureEnd()

        then:
        1 * eventConsumer.onFeatureStart()

        then:
        1 * eventConsumer.onValue(["id"], "21", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onObjectStart(["geometry"], Optional.of(SimpleFeatureGeometry.MULTI_POINT))

        then:
        1 * eventConsumer.onArrayStart(["geometry"])

        then:
        1 * eventConsumer.onValue(["geometry"], "6.406233970262905", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onValue(["geometry"], "50.1501333536934", SchemaBase.Type.FLOAT)

        then:
        1 * eventConsumer.onArrayEnd()

        then:
        1 * eventConsumer.onObjectEnd()

        then:
        1 * eventConsumer.onValue(["kennung"], "631510001-1", SchemaBase.Type.STRING)

        then:
        1 * eventConsumer.onFeatureEnd()

        then:
        1 * eventConsumer.onEnd()

    }
}
