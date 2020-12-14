/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.geojson.app

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.javadsl.Source
import akka.testkit.javadsl.TestKit
import akka.util.ByteString
import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.features.geojson.domain.GeoJsonStreamParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Ignore
import spock.lang.Specification

import static groovy.io.FileType.FILES

/**
 * @author zahnen
 */
@Ignore//TODO
class GeoJsonStreamParserSpec extends Specification {

    static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonStreamParserSpec.class)

    def system = ActorSystem.create()
    def materializer = ActorMaterializer.create(system)
    def featureConsumer = new FeatureConsumerToString()
    def parser = GeoJsonStreamParser.consume(featureConsumer)

    def cleanup() {
        TestKit.shutdownActorSystem(system)
        system = null
    }

    //TODO nesting, geometry types, ...

    //TODO Spec for FeatureTransformerFromGeoJson

    //TODO FeatureTransformerFromGeoJson could be part of AbstractStreamingGeoJsonGraphStage

    //TODO extract logic from AbstractStreamingGeoJsonGraphStage, so that it can be tested with plain JsonParser without akka

    def 'Streaming GeoJson parser (#name)'() {

        given: "GeoJson collection"

        def source = toSource(input)

        when: "parsed"

        source.runWith(parser, materializer)
                .toCompletableFuture()
                .join();

        def actual = featureConsumer.log.toString()

        LOGGER.debug("{}", actual);

        then: 'should match expected'
        actual == expected

        where:
        [name, input] << getTestData()
        expected << getExpected()
    }

    def getTestData() {
        def dataDir = new File('src/test/resources')

        def data = []
        dataDir.traverse(type: FILES, nameFilter: ~/.*\.json$/, sort: {a,b -> a.name <=> b.name}) {
            data << [it.name, it.text]
        }

        return data
    }

    def getExpected() {
        def dataDir = new File('src/test/resources')

        def data = []
        dataDir.traverse(type: FILES, nameFilter: ~/.*_expected\.txt$/, sort: {a,b -> a.name <=> b.name}) {
            data << it.text
        }

        return data
    }

    def toSource(String text) {
        return Source.from(ImmutableList.of(ByteString.fromString(text)))
    }
}
