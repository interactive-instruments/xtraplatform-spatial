/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.geojson.app

import akka.actor.ActorSystem
import akka.stream.javadsl.Source
import akka.testkit.javadsl.TestKit
import akka.util.ByteString
import com.typesafe.config.Config
import de.ii.xtraplatform.features.domain.*
import de.ii.xtraplatform.features.geojson.domain.FeatureTokenDecoderGeoJson
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

/**
 * @author zahnen
 */
class FeatureTokenDecoderGeoJsonSpec extends Specification {

    static final Logger LOGGER = LoggerFactory.getLogger(FeatureTokenDecoderGeoJsonSpec.class)

    @Shared
    ActorSystem system
    @Shared
    Reactive reactive
    @Shared
    Reactive.Runner runner

    FeatureEventHandlerGeneric eventConsumer
    FeatureEventHandlerGeneric loggingEventConsumer
    FeatureTokenDecoder<byte[]> decoder
    FeatureEventHandlerGeneric.GenericContext context

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
        eventConsumer = Mock({
            createContext() >> { ModifiableGenericContext.create()}
        })
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

        decoder = new FeatureTokenDecoderGeoJson()
        decoder.fuse(new Reactive.TranformerCustomFuseableIn<Object, Object, FeatureEventHandlerGeneric>() {
            @Override
            FeatureEventHandlerGeneric fuseableSink() {
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
        1 * eventConsumer.onStart({ FeatureEventHandler.ModifiableContext context ->
            context.metadata().isSingleFeature()
        })

        then:
        1 * eventConsumer.onFeatureStart(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "24"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "611320001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onFeatureEnd(_)

        then:
        1 * eventConsumer.onEnd(_)

    }

    def 'single feature with point geometry'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/simple_point.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart({ FeatureEventHandler.ModifiableContext context ->
            context.metadata().isSingleFeature()
        })

        then:
        1 * eventConsumer.onFeatureStart(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "24"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.geometryType() == Optional.of(SimpleFeatureGeometry.POINT)
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "8.18523495507722"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "49.698295103021096"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onObjectEnd(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "611320001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onFeatureEnd(_)

        then:
        1 * eventConsumer.onEnd(_)

    }

    def 'single feature with multipoint geometry'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/simple_multipoint.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart({ FeatureEventHandler.ModifiableContext context ->
            context.metadata().isSingleFeature()
        })

        then:
        1 * eventConsumer.onFeatureStart(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "20"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.geometryType() == Optional.of(SimpleFeatureGeometry.MULTI_POINT)
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "6.406233970262905"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "50.1501333536934"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "7.406233970262905"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "51.1501333536934"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onObjectEnd(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "580410003-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onFeatureEnd(_)

        then:
        1 * eventConsumer.onEnd(_)

    }

    def 'single feature with multipolygon geometry'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/simple_multipolygon.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart({ FeatureEventHandler.ModifiableContext context ->
            context.metadata().isSingleFeature()
        })

        then:
        1 * eventConsumer.onFeatureStart(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "21"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.geometryType() == Optional.of(SimpleFeatureGeometry.MULTI_POLYGON)
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "8.18523495507722"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "49.698295103021096"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "8.185283687843047"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "49.69823291309017"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "8.185681115675656"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "49.698286680057166"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "8.185796151881165"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "49.69836248910692"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "8.186313615874417"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "49.698603368350874"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "8.18641074595947"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "49.69866280390489"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onObjectEnd(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "631510001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onFeatureEnd(_)

        then:
        1 * eventConsumer.onEnd(_)

    }

    def 'single feature with nested object'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/nested_object.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart({ FeatureEventHandler.ModifiableContext context ->
            context.metadata().isSingleFeature()
        })

        then:
        1 * eventConsumer.onFeatureStart(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "24"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["erfasser"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["erfasser", "name"]
            context.value() == "John Doe"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onObjectEnd(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "611320001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onFeatureEnd(_)

        then:
        1 * eventConsumer.onEnd(_)

    }

    def 'single feature with value array'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/array_value.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart({ FeatureEventHandler.ModifiableContext context ->
            context.metadata().isSingleFeature()
        })

        then:
        1 * eventConsumer.onFeatureStart(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "24"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["erfasser_array"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["erfasser_array"]
            context.value() == "John Doe"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["erfasser_array"]
            context.value() == "Jane Doe"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "611320001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onFeatureEnd(_)

        then:
        1 * eventConsumer.onEnd(_)

    }

    def 'single feature with nested object arrays and value arrays'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/nested_array_object.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart({ FeatureEventHandler.ModifiableContext context ->
            context.metadata().isSingleFeature()
        })

        then:
        1 * eventConsumer.onFeatureStart(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "24"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz"]
        })

        then:
        1 * eventConsumer.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz"]
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe"]
        })

        then:
        1 * eventConsumer.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe", "kreisschluessel"]
            context.value() == "11"
            context.valueType() == SchemaBase.Type.INTEGER
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"]
            context.value() == "34"
            context.valueType() == SchemaBase.Type.INTEGER
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onObjectEnd(_)

        then:
        1 * eventConsumer.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe"]
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"]
            context.value() == "35"
            context.valueType() == SchemaBase.Type.INTEGER
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"]
            context.value() == "36"
            context.valueType() == SchemaBase.Type.INTEGER
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onObjectEnd(_)

        then:
        1 * eventConsumer.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe", "kreisschluessel"]
            context.value() == "12"
            context.valueType() == SchemaBase.Type.INTEGER
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["raumreferenz", "ortsangabe", "flurstueckskennzeichen"]
            context.value() == "37"
            context.valueType() == SchemaBase.Type.INTEGER
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onObjectEnd(_)

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onObjectEnd(_)

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "611320001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onFeatureEnd(_)

        then:
        1 * eventConsumer.onEnd(_)

    }

    def 'collection with 3 features'() {

        given:

        Reactive.Stream<Void> stream = FileSource('src/test/resources/collection.json')
                .via(decoder)
                .to(Reactive.Sink.ignore())

        when:

        runStream(stream)

        then:
        1 * eventConsumer.onStart({ FeatureEventHandler.ModifiableContext context ->
            context.metadata().getNumberReturned() == OptionalLong.of(3)
            context.metadata().getNumberMatched() == OptionalLong.of(12)
        })

        then:
        1 * eventConsumer.onFeatureStart(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "19"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.geometryType() == Optional.of(SimpleFeatureGeometry.POINT)
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "6.295202392345018"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "50.11336914792363"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onObjectEnd(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "580340001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onFeatureEnd(_)

        then:
        1 * eventConsumer.onFeatureStart(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "20"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "580410003-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onFeatureEnd(_)

        then:
        1 * eventConsumer.onFeatureStart(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["id"]
            context.value() == "21"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onObjectStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.geometryType() == Optional.of(SimpleFeatureGeometry.MULTI_POINT)
        })

        then:
        1 * eventConsumer.onArrayStart({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "6.406233970262905"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["geometry"]
            context.value() == "50.1501333536934"
            context.valueType() == SchemaBase.Type.FLOAT
        })

        then:
        1 * eventConsumer.onArrayEnd(_)

        then:
        1 * eventConsumer.onObjectEnd(_)

        then:
        1 * eventConsumer.onValue({ FeatureEventHandler.ModifiableContext context ->
            context.path() == ["kennung"]
            context.value() == "631510001-1"
            context.valueType() == SchemaBase.Type.STRING
        })

        then:
        1 * eventConsumer.onFeatureEnd(_)

        then:
        1 * eventConsumer.onEnd(_)

    }
}
