package de.ii.xtraplatform.crs.infra

import de.ii.xtraplatform.crs.domain.CoordinateTuple
import de.ii.xtraplatform.crs.domain.EpsgCrs
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

class GeoToolsCrsTransformerSpec extends Specification{

    @Shared
    GeoToolsCrsTransformerFactory transformerFactory

    def setupSpec() {
        transformerFactory = new GeoToolsCrsTransformerFactory()
    }

    @Ignore
    def 'CRS transformer test: source and/or target CRS are null'() {
        when:
        def transformer = transformerFactory.getTransformer(sourceCrs, targetCrs)

        then:
        !transformer.isPresent()

        where:
        src          | trgt         | sourceCrs         | targetCrs
        "EPSG:5555"  | "null"       | EpsgCrs.of(5555)  | null
        "null"       | "EPSG:5555"  | null              | EpsgCrs.of(5555)
        "null"       | "null"       | null              | null

    }


    def 'CRS transformer test 3D'() {
        given:
            EpsgCrs sourceCrs = EpsgCrs.of(5555)
            EpsgCrs targetCrs = EpsgCrs.of(4979)
            double[] source = [420735.071, 5392914.343, 131.96]

        when:
            GeoToolsCrsTransformer gct = (GeoToolsCrsTransformer) transformerFactory.getTransformer(sourceCrs, targetCrs).get()
            double[] target = gct.transform3d(source, 1, false)

        then:
            target == source
    }

    def 'CRS transformer test 2D'() {
        given:
            EpsgCrs sourceCrs = EpsgCrs.of(4326)
            EpsgCrs targetCrs = EpsgCrs.of(3857)
            double x = 50.7164
            double y = 7.086
            double rx = 788809.9117611365
            double ry = 6571280.658009371
            double [] ra = new double[10]
            for (int i = 0; i < 10; i += 2){
                ra[i] = x
                ra[i+1] = y
            }

        when:
            GeoToolsCrsTransformer gct = (GeoToolsCrsTransformer) transformerFactory.getTransformer(sourceCrs, targetCrs).get()
            CoordinateTuple coordinateTuple1 = gct.transform(x, y)
            CoordinateTuple coordinateTuple2 = gct.transform(new CoordinateTuple(x ,y), false)
            double [] re = gct.transform(ra, 5, false)

        then:
            coordinateTuple1.getX() == rx
            coordinateTuple1.getY() == ry
            coordinateTuple2.getX() == rx
            coordinateTuple2.getY() == ry
            for (int i = 0; i < 10; i += 2) {
                re[i] == rx
                re[i+1] == ry
            }
    }

}
