/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.json.app


import de.ii.xtraplatform.features.domain.FeatureReaderGeneric
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

/**
 * @author zahnen
 */
class GeoJsonParserSpec extends Specification {

    static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonParserSpec.class)

    FeatureReaderGeneric featureReader = Mock()
    GeoJsonParser parser = new GeoJsonParser(featureReader)

    def 'single feature'() {

        given:

        def source = new File('src/test/resources/simple.json').text

        when:

        parser.parse(source)

        then:
        1 * featureReader.onStart(*_)

        then:
        1 * featureReader.onFeatureStart([], {})

        then:
        1 * featureReader.onValue(["id"], "24", {})

        then:
        1 * featureReader.onValue(["kennung"], "611320001-1", {})

        then:
        1 * featureReader.onFeatureEnd([])

        then:
        1 * featureReader.onEnd()

    }

    def 'single feature with point geometry'() {

        given:

        def source = new File('src/test/resources/simple_point.json').text

        when:

        parser.parse(source)

        then:
        1 * featureReader.onStart(*_)

        then:
        1 * featureReader.onFeatureStart([], {})

        then:
        1 * featureReader.onValue(["id"], "24", {})

        then:
        1 * featureReader.onObjectStart(["geometry"], ['type':'GEOMETRY', "geometryType": "POINT"])

        then:
        1 * featureReader.onArrayStart(["geometry"], {})

        then:
        1 * featureReader.onValue(["geometry"], "8.18523495507722", {})

        then:
        1 * featureReader.onValue(["geometry"], "49.698295103021096", {})

        then:
        1 * featureReader.onArrayEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onObjectEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onValue(["kennung"], "611320001-1", {})

        then:
        1 * featureReader.onFeatureEnd([])

        then:
        1 * featureReader.onEnd()

    }

    def 'single feature with multipoint geometry'() {

        given:

        def source = new File('src/test/resources/simple_multipoint.json').text

        when:

        parser.parse(source)

        then:
        1 * featureReader.onStart(*_)

        then:
        1 * featureReader.onFeatureStart([], {})

        then:
        1 * featureReader.onValue(["id"], "20", {})

        then:
        1 * featureReader.onObjectStart(["geometry"], ['type':'GEOMETRY', "geometryType": "MULTI_POINT"])

        then:
        1 * featureReader.onArrayStart(["geometry"], {})

        then:
        1 * featureReader.onValue(["geometry"], "6.406233970262905", {})

        then:
        1 * featureReader.onValue(["geometry"], "50.1501333536934", {})

        then:
        1 * featureReader.onValue(["geometry"], "7.406233970262905", {})

        then:
        1 * featureReader.onValue(["geometry"], "51.1501333536934", {})

        then:
        1 * featureReader.onArrayEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onObjectEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onValue(["kennung"], "580410003-1", {})

        then:
        1 * featureReader.onFeatureEnd([])

        then:
        1 * featureReader.onEnd()

    }

    def 'single feature with multipolygon geometry'() {

        given:

        def source = new File('src/test/resources/simple_multipolygon.json').text

        when:

        parser.parse(source)

        then:
        1 * featureReader.onStart(*_)

        then:
        1 * featureReader.onFeatureStart([], {})

        then:
        1 * featureReader.onValue(["id"], "21", {})

        then:
        1 * featureReader.onObjectStart(["geometry"], ['type':'GEOMETRY', "geometryType": "MULTI_POLYGON"])

        then:
        1 * featureReader.onArrayStart(["geometry"], {})

        then:
        1 * featureReader.onArrayStart(["geometry"], {})

        then:
        1 * featureReader.onArrayStart(["geometry"], {})

        then:
        1 * featureReader.onValue(["geometry"], "8.18523495507722", {})

        then:
        1 * featureReader.onValue(["geometry"], "49.698295103021096", {})

        then:
        1 * featureReader.onValue(["geometry"], "8.185283687843047", {})

        then:
        1 * featureReader.onValue(["geometry"], "49.69823291309017", {})

        then:
        1 * featureReader.onArrayEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onArrayEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onArrayStart(["geometry"], {})

        then:
        1 * featureReader.onArrayStart(["geometry"], {})

        then:
        1 * featureReader.onValue(["geometry"], "8.185681115675656", {})

        then:
        1 * featureReader.onValue(["geometry"], "49.698286680057166", {})

        then:
        1 * featureReader.onValue(["geometry"], "8.185796151881165", {})

        then:
        1 * featureReader.onValue(["geometry"], "49.69836248910692", {})

        then:
        1 * featureReader.onArrayEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onArrayStart(["geometry"], {})

        then:
        1 * featureReader.onValue(["geometry"], "8.186313615874417", {})

        then:
        1 * featureReader.onValue(["geometry"], "49.698603368350874", {})

        then:
        1 * featureReader.onValue(["geometry"], "8.18641074595947", {})

        then:
        1 * featureReader.onValue(["geometry"], "49.69866280390489", {})

        then:
        1 * featureReader.onArrayEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onArrayEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onArrayEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onObjectEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onValue(["kennung"], "631510001-1", {})

        then:
        1 * featureReader.onFeatureEnd([])

        then:
        1 * featureReader.onEnd()

    }

    def 'single feature with nested object'() {

        given:

        def source = new File('src/test/resources/nested_object.json').text

        when:

        parser.parse(source)

        then:
        1 * featureReader.onStart(*_)

        then:
        1 * featureReader.onFeatureStart([], {})

        then:
        1 * featureReader.onValue(["id"], "24", {})

        then:
        1 * featureReader.onObjectStart(["erfasser"], {})

        then:
        1 * featureReader.onValue(["erfasser", "name"], "John Doe", {})

        then:
        1 * featureReader.onObjectEnd(["erfasser"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onValue(["kennung"], "611320001-1", {})

        then:
        1 * featureReader.onFeatureEnd([])

        then:
        1 * featureReader.onEnd()

    }

    def 'single feature with value array'() {

        given:

        def source = new File('src/test/resources/array_value.json').text

        when:

        parser.parse(source)

        then:
        1 * featureReader.onStart(*_)

        then:
        1 * featureReader.onFeatureStart([], {})

        then:
        1 * featureReader.onValue(["id"], "24", {})

        then:
        1 * featureReader.onArrayStart(["erfasser_array"], {})

        then:
        1 * featureReader.onValue(["erfasser_array"], "John Doe", {})

        then:
        1 * featureReader.onValue(["erfasser_array"], "Jane Doe", {})

        then:
        1 * featureReader.onArrayEnd(["erfasser_array"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onValue(["kennung"], "611320001-1", {})

        then:
        1 * featureReader.onFeatureEnd([])

        then:
        1 * featureReader.onEnd()

    }

    def 'single feature with nested object arrays and value arrays'() {

        given:

        def source = new File('src/test/resources/nested_array_object.json').text

        when:

        parser.parse(source)

        then:
        1 * featureReader.onStart(*_)

        then:
        1 * featureReader.onFeatureStart([], {})

        then:
        1 * featureReader.onValue(["id"], "24", {})

        then:
        1 * featureReader.onArrayStart(["raumreferenz"], {})

        then:
        1 * featureReader.onObjectStart(["raumreferenz"], {})

        then:
        1 * featureReader.onArrayStart(["raumreferenz", "ortsangabe"], {})

        then:
        1 * featureReader.onObjectStart(["raumreferenz", "ortsangabe"], {})

        then:
        1 * featureReader.onValue(["raumreferenz", "ortsangabe", "kreisschluessel"], "11", {})

        then:
        1 * featureReader.onArrayStart(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], {})

        then:
        1 * featureReader.onValue(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], "34", {})

        then:
        1 * featureReader.onArrayEnd(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onObjectEnd(["raumreferenz", "ortsangabe"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onObjectStart(["raumreferenz", "ortsangabe"], {})

        then:
        1 * featureReader.onArrayStart(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], {})

        then:
        1 * featureReader.onValue(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], "35", {})

        then:
        1 * featureReader.onValue(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], "36", {})

        then:
        1 * featureReader.onArrayEnd(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onObjectEnd(["raumreferenz", "ortsangabe"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onObjectStart(["raumreferenz", "ortsangabe"], {})

        then:
        1 * featureReader.onValue(["raumreferenz", "ortsangabe", "kreisschluessel"], "12", {})

        then:
        1 * featureReader.onArrayStart(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], {})

        then:
        1 * featureReader.onValue(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], "37", {})

        then:
        1 * featureReader.onArrayEnd(["raumreferenz", "ortsangabe", "flurstueckskennzeichen"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onObjectEnd(["raumreferenz", "ortsangabe"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onArrayEnd(["raumreferenz", "ortsangabe"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onObjectEnd(["raumreferenz"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onArrayEnd(["raumreferenz"], com.google.common.collect.ImmutableMap.of())

        then:
        1 * featureReader.onValue(["kennung"], "611320001-1", {})

        then:
        1 * featureReader.onFeatureEnd([])

        then:
        1 * featureReader.onEnd()

    }

    def 'collection with 3 features'() {

        given:

        def source = new File('src/test/resources/collection.json').text

        when:

        parser.parse(source)

        then:
        1 * featureReader.onStart(OptionalLong.of(3), OptionalLong.of(12), {})

        then:
        1 * featureReader.onFeatureStart([], {})

        then:
        1 * featureReader.onValue(["id"], "19", {})

        then:
        1 * featureReader.onObjectStart(["geometry"], ['type':'GEOMETRY', "geometryType": "POINT"])

        then:
        1 * featureReader.onArrayStart(["geometry"], {})

        then:
        1 * featureReader.onValue(["geometry"], "6.295202392345018", {})

        then:
        1 * featureReader.onValue(["geometry"], "50.11336914792363", {})

        then:
        1 * featureReader.onArrayEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onObjectEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onValue(["kennung"], "580340001-1", {})

        then:
        1 * featureReader.onFeatureEnd([])

        then:
        1 * featureReader.onFeatureStart([], {})

        then:
        1 * featureReader.onValue(["id"], "20", {})

        then:
        1 * featureReader.onValue(["kennung"], "580410003-1", {})

        then:
        1 * featureReader.onFeatureEnd([])

        then:
        1 * featureReader.onFeatureStart([], {})

        then:
        1 * featureReader.onValue(["id"], "21", {})

        then:
        1 * featureReader.onObjectStart(["geometry"], ['type':'GEOMETRY', "geometryType": "MULTI_POINT"])

        then:
        1 * featureReader.onArrayStart(["geometry"], {})

        then:
        1 * featureReader.onValue(["geometry"], "6.406233970262905", {})

        then:
        1 * featureReader.onValue(["geometry"], "50.1501333536934", {})

        then:
        1 * featureReader.onArrayEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onObjectEnd(["geometry"], ['type':'GEOMETRY'])

        then:
        1 * featureReader.onValue(["kennung"], "631510001-1", {})

        then:
        1 * featureReader.onFeatureEnd([])

        then:
        1 * featureReader.onEnd()

    }
}
