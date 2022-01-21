/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.json.app


import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author zahnen
 */
@Ignore//TODO
class FeatureTransformerFromGeoJsonSpec extends Specification {

    /*static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerFromGeoJsonSpec.class)

    def system = ActorSystem.create()
    def materializer = ActorMaterializer.create(system)
    def featureTransformer = new FeatureTransformerToString()
    def mapping = getTestMapping()
    def parser = GeoJsonStreamParser.transform(mapping, featureTransformer)

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

        def actual = featureTransformer.toString()

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
        dataDir.traverse(type: FILES, nameFilter: ~/.*\.json$/, sort: { a, b -> a.name <=> b.name }) {
            data << [it.name, it.text]
        }

        return data
    }

    def getExpected() {
        def dataDir = new File('src/test/resources')

        def data = []
        dataDir.traverse(type: FILES, nameFilter: ~/.*_expected_tr\.txt$/, sort: { a, b -> a.name <=> b.name }) {
            data << it.text
        }

        return data
    }

    def toSource(String text) {
        return Source.from(ImmutableList.of(ByteString.fromString(text)))
    }

    static def getTestMapping() {
        return new ImmutableFeatureTypeMapping.Builder()
                .putMappings("id",
                        new ImmutableSourcePathMapping.Builder()
                                .putMappings("SQL", new MappingSwapper.MappingReadFromWrite("/fundortpflanzen/[id=id]osirisobjekt/id", null))
                                .build())
                .putMappings("kennung",
                        new ImmutableSourcePathMapping.Builder()
                                .putMappings("SQL", new MappingSwapper.MappingReadFromWrite("/fundortpflanzen/[id=id]osirisobjekt/kennung", null))
                                .build())
                .putMappings("erfasser/name",
                        new ImmutableSourcePathMapping.Builder()
                                .putMappings("SQL", new MappingSwapper.MappingReadFromWrite("/fundortpflanzen/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser[erfasser]/name", null))
                                .build())
                .putMappings("erfasser_array",
                        new ImmutableSourcePathMapping.Builder()
                                .putMappings("SQL", new MappingSwapper.MappingReadFromWrite("/fundortpflanzen/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser[erfasser]/name", null))
                                .build())
                .putMappings("geometry",
                        new ImmutableSourcePathMapping.Builder()
                                .putMappings("SQL", new MappingSwapper.MappingReadFromWrite("/fundortpflanzen/[id=id]artbeobachtung/[geom=id]geom/geom", null))
                                .build())

                .putMappings("raumreferenz/ortsangabe/kreisschluessel",
                        new ImmutableSourcePathMapping.Builder()
                                .putMappings("SQL", new MappingSwapper.MappingReadFromWrite("/fundortpflanzen/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel", null))
                                .build())
                .putMappings("raumreferenz/ortsangabe/flurstueckskennzeichen",
                        new ImmutableSourcePathMapping.Builder()
                                .putMappings("SQL", new MappingSwapper.MappingReadFromWrite("/fundortpflanzen/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen", null))
                                .build())
                .build();
    }*/
}
