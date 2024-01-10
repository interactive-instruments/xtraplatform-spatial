/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.json.app

import de.ii.xtraplatform.features.domain.FeatureEventHandler
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder
import de.ii.xtraplatform.features.domain.FeatureTokenFixtures
import de.ii.xtraplatform.features.domain.SchemaMapping
import de.ii.xtraplatform.features.json.domain.FeatureTokenDecoderGeoJson
import de.ii.xtraplatform.streams.app.ReactiveRx
import de.ii.xtraplatform.streams.domain.Reactive
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author zahnen
 */
class FeatureTokenDecoderGeoJsonSpec2 extends Specification {

    static final Logger LOGGER = LoggerFactory.getLogger(FeatureTokenDecoderGeoJsonSpec2.class)

    @Shared
    Reactive reactive
    @Shared
    Reactive.Runner runner

    FeatureTokenDecoder<byte[], FeatureSchema, SchemaMapping, FeatureEventHandler.ModifiableContext<FeatureSchema, SchemaMapping>> decoder

    def setupSpec() {
        reactive = new ReactiveRx()
        runner = reactive.runner("test")
    }

    def cleanupSpec() {
        runner.close()
    }

    def setup() {
        decoder = new FeatureTokenDecoderGeoJson("biotop")
    }

    public <T> T runStream(Reactive.Stream<T> stream) {
        def result = stream.on(runner).run().toCompletableFuture().join()
        println(result.toString())
        return result
    }

    static Reactive.Source<byte[]> FileSource(String path) {
        Reactive.Source.inputStream(new File(path).newInputStream())
    }

    static Reactive.SinkReduced<Object, List<Object>> ListSink() {
        Reactive.SinkReduced.reduce([], (list2, element) -> {
            list2 << element
            return list2
        })
    }

    def 'single feature'() {

        given:

        Reactive.Stream<List<Object>> stream = FileSource('src/test/resources/simple.json')
                .via(decoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == FeatureTokenFixtures.SINGLE_FEATURE_SOURCE

    }

    def 'single feature with point geometry'() {

        given:

        Reactive.Stream<List<Object>> stream = FileSource('src/test/resources/simple_point.json')
                .via(decoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == FeatureTokenFixtures.SINGLE_FEATURE_POINT_SOURCE

    }

    def 'single feature with multipoint geometry'() {

        given:

        Reactive.Stream<List<Object>> stream = FileSource('src/test/resources/simple_multipoint.json')
                .via(decoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == FeatureTokenFixtures.SINGLE_FEATURE_MULTI_POINT_SOURCE

    }

    def 'single feature with multipolygon geometry'() {

        given:

        Reactive.Stream<List<Object>> stream = FileSource('src/test/resources/simple_multipolygon.json')
                .via(decoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == FeatureTokenFixtures.SINGLE_FEATURE_MULTI_POLYGON_SOURCE

    }

    def 'single feature with nested object'() {

        given:

        Reactive.Stream<List<Object>> stream = FileSource('src/test/resources/nested_object.json')
                .via(decoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == FeatureTokenFixtures.SINGLE_FEATURE_NESTED_OBJECT_SOURCE

    }

    def 'single feature with value array'() {

        given:

        Reactive.Stream<List<Object>> stream = FileSource('src/test/resources/array_value.json')
                .via(decoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == FeatureTokenFixtures.SINGLE_FEATURE_VALUE_ARRAY_SOURCE

    }

    def 'single feature with nested object arrays and value arrays'() {

        given:

        Reactive.Stream<List<Object>> stream = FileSource('src/test/resources/nested_array_object.json')
                .via(decoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == FeatureTokenFixtures.SINGLE_FEATURE_NESTED_OBJECT_ARRAYS_SOURCE

    }

    def 'collection with 3 features'() {

        given:

        Reactive.Stream<List<Object>> stream = FileSource('src/test/resources/collection.json')
                .via(decoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == FeatureTokenFixtures.COLLECTION_SOURCE

    }
}
