/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.csw.client;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.ogc.csw.parser.ExtractWFSUrlsFromCSW;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.staxmate.SMInputFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

/**
 * @author zahnen
 */
public class CSWAdapterTest {

    @BeforeClass(groups = {"default"})
    public void setUp() {
    }

    @Test(groups = {"default"})
    public void testSomeMethod() {

        // http://geoportal.bayern.de/csw/bvv%20?service=CSW&version=2.0.2&request=GetRecords&namespace=xmlns(csw=http://www.opengis.net/cat/csw/2.0.2),xmlns(gmd=http://www.isotc211.org/2005/gmd)&resultType=results&outputFormat=application/xml%20&outputSchema=http://www.isotc211.org/2005/gmd%20&startPosition=1&maxRecords=20%20&typeNames=csw:Record%20&elementSetName=full&constraintLanguage=CQL_TEXT%20&constraint_language_version=1.1.0%20&constraint=csw:AnyText=%27WFS%27
        // https://apps.geoportal.nrw.de/soapServices/CSWStartup?Service=CSW&Request=GetCapabilities&Version=2.0.2
        // ServiceType


        // OK
        List<String> urls = ImmutableList.of(
                //"http://geoportal.bayern.de/csw/bvv" // 8
                //"http://gdk.gdi-de.org/gdi-de/srv/eng/csw" // 2595
                //"https://geometadaten.lfrz.at/at.lfrz.discoveryservices/srv/de/csw202" // 2
                //"http://www.ign.es/csw-inspire/srv/eng/csw" // 3, has some more than can't be detected
                //"http://csw.geo.be/eng/csw" // 7, verified
                //"http://www.geodata-info.dk/registrant/srv/en/csw" // 17
                //"http://csw.data.gov.uk/geonetwork/srv/en/csw" // 96
                //"http://www.paikkatietohakemisto.fi/geonetwork/srv/eng/csw" // 0, wrong operation name, e.g. SYKEn INSPIRE-latauspalvelu, BETA
                //"https://inspirebg.eu/geonetwork/srv/eng/csw" // 0
                //"https://www.nationaalgeoregister.nl/geonetwork/srv/dut/csw-inspire" // 44
                "https://nationaalgeoregister.nl/geonetwork/srv/dut/csw"
                //"http://snig.dgterritorio.pt/geoportal/csw/discovery" // 129
                //"http://inspire.maaamet.ee/geoportal/csw/discovery" // 2, some more with wrong operation name, e.g. Eesti Corine andmete allalaadimisteenus
        );

        // with issues
        List<String> urls2 = ImmutableList.of(
                //"http://www.geoportale.isprambiente.it/geoportale/csw" // 0, WFS URL and only mention in distributionInfo linkage and protocol
                //"http://mapy.geoportal.gov.pl/wss/service/CSWINSP/guest/CSWStartup" // ??? missing Content-Type header
                //"https://www.geodata.se/InspireCswProxy/csw" // 0, URIException on GET, works per POST

        );

        ExtractWFSUrlsFromCSW urlsFromCSW = new ExtractWFSUrlsFromCSW(new DefaultHttpClient(), new SMInputFactory(new InputFactoryImpl()));


        for (String url : urls) {
            //urlsFromCSW.caps(url);
            urlsFromCSW.extract(url);
        }
    }
}
