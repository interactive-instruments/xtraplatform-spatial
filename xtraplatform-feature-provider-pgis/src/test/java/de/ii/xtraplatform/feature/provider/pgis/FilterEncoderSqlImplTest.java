/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import org.geotools.filter.text.cql2.CQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author zahnen
 */
public class FilterEncoderSqlImplTest {
    /*
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterEncoderSqlImplTest.class);

    FeatureTypeMapping mappings = new ImmutableFeatureTypeMapping.Builder().build();

    ///fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name LIKE '*Mat*'
    //fundorttiere.id IN (SELECT artbeobachtung_2_erfasser.artbeobachtung_id FROM artbeobachtung_2_erfasser JOIN erfasser ON artbeobachtung_2_erfasser.erfasser_id=erfasser.id WHERE erfasser.name ILIKE '%Mat%' )
    @Test(groups = {"default"})
    public void testEncodeFilter() throws CQLException {
        String path = "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name";
        String input = path + " LIKE '*Mat*'" + " AND " + path + " = 5";

        ImmutableSqlFeatureQueries queries = ImmutableSqlFeatureQueries.builder()
                                                                       .addPaths(path)
                                                                       .build();

        String encodedFilter = new FeatureQueryEncoderSql(queries, mappings).encodeFilter(input);
        LOGGER.debug("ENCODED {}", encodedFilter);

        String expected = "((fundorttiere.id IN (SELECT artbeobachtung_2_erfasser.artbeobachtung_id FROM artbeobachtung_2_erfasser JOIN erfasser ON artbeobachtung_2_erfasser.erfasser_id=erfasser.id WHERE erfasser.name LIKE '%Mat%')) AND (fundorttiere.id IN (SELECT artbeobachtung_2_erfasser.artbeobachtung_id FROM artbeobachtung_2_erfasser JOIN erfasser ON artbeobachtung_2_erfasser.erfasser_id=erfasser.id WHERE erfasser.name = '5')))";

        assertEquals(encodedFilter, expected);
    }

    @Test(groups = {"default"})
    public void testEncodeFilter2() throws CQLException {
        String path = "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/st_astext(st_forceccw(geom))";
        String input = "BBOX(" + path + ", 441243.8931, 5505405.2643,441247.3324, 5505398.3121)";

        ImmutableSqlFeatureQueries queries = ImmutableSqlFeatureQueries.builder()
                                                                       .addPaths(path)
                                                                       .build();

        String encodedFilter = new FeatureQueryEncoderSql(queries, mappings).encodeFilter(input);
        LOGGER.debug("ENCODED {}", encodedFilter);

        String expected = "((fundorttiere.id IN (SELECT artbeobachtung.id FROM artbeobachtung JOIN geom ON artbeobachtung.geom=geom.id WHERE ST_Intersects(geom.geom, ST_GeomFromText('POLYGON((441243.8931 5505398.3121,441247.3324 5505398.3121,441247.3324 5505405.2643,441243.8931 5505405.2643,441243.8931 5505398.3121))',25832)) = 'TRUE')))";

        assertEquals(encodedFilter, expected);
    }

    @Test(groups = {"default"})
    public void testEncodeFilter3() throws CQLException {
        String path = "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam";
        String path2 = "fundorttiere.osirisobjekt.veroeffentlichtam";
        String input = path2 + " DURING 1970-01-01T00:00:00Z/2018-07-17T07:14:27Z";

        ImmutableSqlFeatureQueries queries = ImmutableSqlFeatureQueries.builder()
                                                                       .addPaths(path)
                                                                       .build();

        String encodedFilter = new FeatureQueryEncoderSql(queries, mappings).encodeFilter(input);
        LOGGER.debug("ENCODED {}", encodedFilter);

        String expected = "((fundorttiere.id IN (SELECT fundorttiere.id FROM fundorttiere JOIN osirisobjekt ON fundorttiere.id=osirisobjekt.id WHERE osirisobjekt.veroeffentlichtam BETWEEN '1970-01-01T00:00:00Z' AND '2018-07-17T07:14:27Z')))";

        assertEquals(encodedFilter, expected);
    }

    @Test(groups = {"default"})
    public void testEncodeFilter4() throws CQLException {
        String path = "/fundorttiere/[id=id]osirisobjekt/veroeffentlichtam";
        String path2 = "fundorttiere.osirisobjekt.veroeffentlichtam";
        String input = path2 + " TEQUALS 2018-07-17T07:14:27Z";

        ImmutableSqlFeatureQueries queries = ImmutableSqlFeatureQueries.builder()
                                                                       .addPaths(path)
                                                                       .build();

        String encodedFilter = new FeatureQueryEncoderSql(queries, mappings).encodeFilter(input);
        LOGGER.debug("ENCODED {}", encodedFilter);

        String expected = "((fundorttiere.id IN (SELECT fundorttiere.id FROM fundorttiere JOIN osirisobjekt ON fundorttiere.id=osirisobjekt.id WHERE osirisobjekt.veroeffentlichtam = '2018-07-17T07:14:27Z')))";

        assertEquals(encodedFilter, expected);
    }



    @Test(groups = {"default"})
    public void testEncodeFilter5() throws CQLException {
        String path = "/fundorttiere/[id=id]artbeobachtung/[geom=id]geom/st_astext(st_forceccw(geom))";
        String path2 = "/fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name";
        String input = "BBOX(" + path + ", 441243.8931, 5505405.2643,441247.3324, 5505398.3121)" + " AND " + path2 + " LIKE '*Mat*'";

        ImmutableSqlFeatureQueries queries = ImmutableSqlFeatureQueries.builder()
                                                                       .addPaths(path, path2)
                                                                       .build();

        String encodedFilter = new FeatureQueryEncoderSql(queries, mappings).encodeFilter(input);
        LOGGER.debug("ENCODED {}", encodedFilter);

        String expected = "((fundorttiere.id IN (SELECT artbeobachtung.id FROM artbeobachtung JOIN geom ON artbeobachtung.geom=geom.id WHERE ST_Intersects(geom.geom, ST_GeomFromText('POLYGON((441243.8931 5505398.3121,441247.3324 5505398.3121,441247.3324 5505405.2643,441243.8931 5505405.2643,441243.8931 5505398.3121))',25832)) = 'TRUE')) AND (fundorttiere.id IN (SELECT artbeobachtung_2_erfasser.artbeobachtung_id FROM artbeobachtung_2_erfasser JOIN erfasser ON artbeobachtung_2_erfasser.erfasser_id=erfasser.id WHERE erfasser.name LIKE '%Mat%')))";

        assertEquals(encodedFilter, expected);
    }

     */
}