/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import com.typesafe.config.Config
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRowFixtures
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema
import de.ii.xtraplatform.streams.app.ReactiveAkka
import de.ii.xtraplatform.streams.domain.ActorSystemProvider
import de.ii.xtraplatform.streams.domain.Reactive
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author zahnen
 */
class FeatureDecoderSqlSpec extends Specification {

    static final Logger LOGGER = LoggerFactory.getLogger(FeatureDecoderSqlSpec.class)

    @Shared
    ActorSystem system
    @Shared
    Reactive reactive
    @Shared
    Reactive.Runner runner

    FeatureTokenDecoder<SqlRow> collectionDecoder
    FeatureTokenDecoder<SqlRow> singleDecoder

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
        collectionDecoder = new FeatureDecoderSql(
                SqlRowFixtures.TYPE_INFOS, new ImmutableFeatureSchema.Builder().name("test").build(),
                ImmutableFeatureQuery.builder()
                        .type("biotop")
                        .build())
        singleDecoder = new FeatureDecoderSql(
                SqlRowFixtures.TYPE_INFOS, new ImmutableFeatureSchema.Builder().name("test").build(),
                ImmutableFeatureQuery.builder()
                        .type("biotop")
                        .returnsSingleFeature(true)
                        .build())
    }

    public <T> T runStream(Reactive.Stream<T> stream) {
        def result = stream.on(runner).run().toCompletableFuture().join()
        println(result.toString())
        return result
    }

    static Reactive.Source<SqlRow> ListSource(List<SqlRow> sqlRows) {
        Reactive.Source.iterable(sqlRows)
    }

    static Reactive.Sink<Object, List<Object>> ListSink() {
        Reactive.Sink.reduce([], (list2, element) -> {
            list2 << element
            return list2
        })
    }

    def 'single feature'() {

        given:

        Reactive.Stream<List<Object>> stream = ListSource(SqlRowFixtures.SINGLE_FEATURE)
                .via(singleDecoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == SqlRowFixtures.SINGLE_FEATURE_TOKENS

    }

    def 'single feature with point geometry'() {

        given:

        Reactive.Stream<List<Object>> stream = ListSource(SqlRowFixtures.SINGLE_FEATURE_POINT)
                .via(singleDecoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == SqlRowFixtures.SINGLE_FEATURE_POINT_TOKENS

    }

    def 'single feature with multipoint geometry'() {

        given:

        Reactive.Stream<List<Object>> stream = ListSource(SqlRowFixtures.SINGLE_FEATURE_MULTI_POINT)
                .via(singleDecoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == SqlRowFixtures.SINGLE_FEATURE_MULTI_POINT_TOKENS

    }

    def 'single feature with multipolygon geometry'() {

        given:

        Reactive.Stream<List<Object>> stream = ListSource(SqlRowFixtures.SINGLE_FEATURE_MULTI_POLYGON)
                .via(singleDecoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == SqlRowFixtures.SINGLE_FEATURE_MULTI_POLYGON_TOKENS

    }

    def 'single feature with nested object'() {

        given:

        Reactive.Stream<List<Object>> stream = ListSource(SqlRowFixtures.SINGLE_FEATURE_NESTED_OBJECT)
                .via(singleDecoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == SqlRowFixtures.SINGLE_FEATURE_NESTED_OBJECT_TOKENS

    }

    def 'single feature with nested object arrays and value arrays'() {

        given:

        Reactive.Stream<List<Object>> stream = ListSource(SqlRowFixtures.SINGLE_FEATURE_NESTED_OBJECT_ARRAYS)
                .via(singleDecoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == SqlRowFixtures.SINGLE_FEATURE_NESTED_OBJECT_ARRAYS_TOKENS

    }

    def 'collection with 3 features'() {

        given:

        Reactive.Stream<List<Object>> stream = ListSource(SqlRowFixtures.COLLECTION)
                .via(collectionDecoder)
                .to(ListSink())

        when:

        List<Object> tokens = runStream(stream)

        then:
        tokens == SqlRowFixtures.COLLECTION_TOKENS

    }
}
