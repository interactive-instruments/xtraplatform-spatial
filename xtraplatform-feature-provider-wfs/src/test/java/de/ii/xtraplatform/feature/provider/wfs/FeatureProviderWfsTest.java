/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.testkit.javadsl.TestKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author zahnen
 */
public class FeatureProviderWfsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfsTest.class);

    static ActorSystem system;
    static ActorMaterializer materializer;

    @BeforeClass(groups = {"default"})
    public static void setup() {
        system = ActorSystem.create();
        materializer = ActorMaterializer.create(system);
    }

    @AfterClass(groups = {"default"})
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test(groups = {"default"})
    public void testGetFeatureStream() throws InterruptedException, ExecutionException, TimeoutException {
        new TestKit(system) {
            {/*
                TestKit probe = new TestKit(system);

                WFSAdapter wfs = new WFSAdapter("http://xtraproxy-testing-3/aaa-suite/cgi-bin/alkis/sf/wfs");
                wfs.setVersion(WFS.VERSION._2_0_0.toString());
                wfs.getNsStore().addNamespace("adv", "http://www.adv-online.de/namespaces/adv/gid/6.0");

                FeatureTypeConfigurationOld featureType = new FeatureTypeConfigurationOld("AX_Bahnstrecke", "http://www.adv-online.de/namespaces/adv/gid/6.0", "adv:AX_Bahnstrecke");
                AkkaHttp akkaHttp = new AkkaHttpMock(ActorMaterializer.create(probe.getSystem()), ByteString.fromString(doc));

                GmlProvider provider = new FeatureProviderWfs(akkaHttp, wfs, ImmutableMap.<String, FeatureTypeConfigurationOld>builder().put("AX_Bahnstrecke", featureType)
                                                                                                                                        .build());

                FeatureQuery query = ImmutableFeatureQuery.builder().type("AX_Bahnstrecke")
                                                          .limit(10)
                                                          .build();

                FeatureStream<GmlConsumer> featureStream = provider.getFeatureStream(query);
                CompletionStage<Done> done = featureStream.apply(new GmlConsumer() {
                    @Override
                    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {

                    }

                    @Override
                    public void onEnd() throws Exception {

                    }

                    @Override
                    public void onFeatureStart(List<String> path) throws Exception {

                    }

                    @Override
                    public void onFeatureEnd(List<String> path) throws Exception {

                    }

                    @Override
                    public void onGmlAttribute(String namespace, String localName, List<String> path, String value) throws Exception {
                        LOGGER.debug("{}: {}", localName, value);
                    }

                    @Override
                    public void onPropertyStart(List<String> path, List<Integer> multiplicities) throws Exception {
                        LOGGER.debug("{}", path);
                    }

                    @Override
                    public void onPropertyText(String text) throws Exception {

                    }

                    @Override
                    public void onPropertyEnd(List<String> path) throws Exception {

                    }

                    @Override
                    public void onNamespaceRewrite(QName featureType, String namespace) throws Exception {

                    }
                });

                done.toCompletableFuture().get(10, TimeUnit.SECONDS);*/
            }
        };
    }

    static final String doc = "\n" +
            //"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<wfs:FeatureCollection timeStamp=\"2018-02-22T14:02:49.031+01:00\" numberReturned=\"10\" numberMatched=\"15674\" xmlns=\"http://www.adv-online.de/namespaces/adv/gid/6.0\" xmlns:gml=\"http://www.opengis.net/gml/3.2\" xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.adv-online.de/namespaces/adv/gid/6.0 http://www.wfs.nrw.de/aaa-suite/schema/NAS/6.0/schema/AAA-Fachschema.xsd http://www.opengis.net/wfs/2.0 http://www.wfs.nrw.de/aaa-suite/schema/ogc/wfs/2.0/wfs.xsd http://www.opengis.net/gml/3.2 http://www.wfs.nrw.de/aaa-suite/schema/NAS/6.0/schema/gml/3.2.1/gml.xsd http://www.isotc211.org/2005/gco http://www.wfs.nrw.de/aaa-suite/schema/NAS/6.0/schema/iso/19139/20070417/gco/gco.xsd http://www.isotc211.org/2005/gmd http://www.wfs.nrw.de/aaa-suite/schema/NAS/6.0/schema/iso/19139/20070417/gmd/gmd.xsd\">\n" +
            "<wfs:boundedBy>\n" +
            "<gml:Envelope srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:lowerCorner>325581.962 5582444.67</gml:lowerCorner>\n" +
            "<gml:upperCorner>334365.596 5595539.828</gml:upperCorner>\n" +
            "</gml:Envelope>\n" +
            "</wfs:boundedBy>\n" +
            "<wfs:member>\n" +
            "<AX_Bahnstrecke gml:id=\"DENWAT01D0007ML7\">\n" +
            "<gml:identifier codeSpace=\"http://www.adv-online.de/\">urn:adv:oid:DENWAT01D0007ML7</gml:identifier>\n" +
            "<lebenszeitintervall>\n" +
            "<AA_Lebenszeitintervall>\n" +
            "<beginnt>2016-11-29T12:14:05Z</beginnt>\n" +
            "</AA_Lebenszeitintervall>\n" +
            "</lebenszeitintervall>\n" +
            "<modellart>\n" +
            "<AA_Modellart>\n" +
            "<advStandardModell>Basis-DLM</advStandardModell>\n" +
            "</AA_Modellart>\n" +
            "</modellart>\n" +
            "<position>\n" +
            "<gml:LineString gml:id=\"o42014.id.297880.position.Geom_0\" srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:posList>326165.967 5584706.836 326144.512 5584671.174 326140.437 5584664.433 326132.871 5584652.002 326127.272 5584643.248 326120.213 5584633.06 326112.583 5584622.968 326104.386 5584612.977 326095.627 5584603.093 326087.911 5584594.939 326079.249 5584586.239 326069.484 5584576.816 326055.818 5584563.969 326048.442 5584557.261 326032.646 5584542.787 325973.516 5584488.63 325887.887 5584412.726 325877.554 5584403.396 325868.019 5584394.43 325859.134 5584385.688 325850.821 5584377.093 325846.531 5584372.463 325841.605 5584366.957 325823.281 5584344.565 325809.901 5584327.805 325798.725 5584313.302 325788.533 5584299.519 325779.144 5584286.212 325770.47 5584273.254 325760.011 5584256.574 325749.898 5584239.214 325740.123 5584221.161 325730.681 5584202.404 325716.882 5584172.938 325707.558 5584150.466 325701.209 5584134.24 325695.828 5584119.21 325690.78 5584103.705 325686.056 5584087.704 325681.65 5584071.18 325677.264 5584052.794 325673.383 5584034.292 325670.009 5584015.689 325667.147 5583997.007 325664.449 5583975.526 325661.116 5583944.31 325646.405 5583802.993 325641.788 5583758.045 325637.538 5583719.686 325634.562 5583693.086 325629.498 5583646.844 325628.21 5583634.897 325610.9 5583535.818 325606.644 5583514.673 325604.899 5583506.137 325598.828 5583476 325595.261 5583458.289 325592.249 5583442.698 325589.665 5583427.955 325587.474 5583413.851 325585.658 5583400.276 325584.202 5583387.098 325583.104 5583374.298 325582.36 5583361.824 325581.969 5583349.64 325581.962 5583332.573 325582.541 5583309.286 325582.991 5583303.063 325583.644 5583295.568 325584.523 5583287.99 325585.63 5583280.327 325586.964 5583272.579 325589.792 5583259.028 325593.342 5583244.933 325597.654 5583230.135 325602.826 5583214.294 325608.158 5583199.323 325613.731 5583184.877 325619.559 5583170.92 325625.65 5583157.429 325631.355 5583145.683 325637.385 5583134.055 325643.737 5583122.556 325650.404 5583111.195 325658.219 5583098.699 325666.069 5583087.02 325674.014 5583076.067 325682.097 5583065.782 325685.149 5583062.056 325711.481 5583032.571 325719.912 5583024.235 325728.432 5583016.174 325745.708 5583000.365 325777.563 5582971.358 325827.294 5582929.02 325837.882 5582919.673 325847.994 5582910.064 325857.622 5582900.202 325866.755 5582890.098 325872.69 5582883.089 325878.766 5582875.559 325891.794 5582858.364 325899.479 5582847.681 325906.727 5582837.222 325913.573 5582826.937 325920.039 5582816.792 325926.304 5582806.509 325932.876 5582795.273 325948.466 5582767.308 325956.578 5582752.317 325962.777 5582740.447 325968.159 5582729.598 325972.856 5582719.487 325976.549 5582710.947 325980.144 5582702.056 325983.66 5582692.773 325987.118 5582683.036 325993.943 5582662.003 326001.162 5582637.282 326007.149 5582615.131 326012.016 5582595.369 326016.021 5582576.941 326019.239 5582559.492 326021.221 5582546.575 326022.94 5582533.171 326024.406 5582519.214 326025.63 5582504.592 326027.156 5582479.006 326028.49 5582444.67</gml:posList>\n" +
            "</gml:LineString>\n" +
            "</position>\n" +
            "<bahnkategorie>1100</bahnkategorie>\n" +
            "<elektrifizierung>2000</elektrifizierung>\n" +
            "<anzahlDerStreckengleise>2000</anzahlDerStreckengleise>\n" +
            "<nummerDerBahnstrecke>2631</nummerDerBahnstrecke>\n" +
            "<spurweite>1000</spurweite>\n" +
            "</AX_Bahnstrecke>\n" +
            "</wfs:member>\n" +
            "<wfs:member>\n" +
            "<AX_Bahnstrecke gml:id=\"DENWAT01D0007MnG\">\n" +
            "<gml:identifier codeSpace=\"http://www.adv-online.de/\">urn:adv:oid:DENWAT01D0007MnG</gml:identifier>\n" +
            "<lebenszeitintervall>\n" +
            "<AA_Lebenszeitintervall>\n" +
            "<beginnt>2017-07-14T09:53:38Z</beginnt>\n" +
            "</AA_Lebenszeitintervall>\n" +
            "</lebenszeitintervall>\n" +
            "<modellart>\n" +
            "<AA_Modellart>\n" +
            "<advStandardModell>Basis-DLM</advStandardModell>\n" +
            "</AA_Modellart>\n" +
            "</modellart>\n" +
            "<position>\n" +
            "<gml:LineString gml:id=\"o42014.id.307618.position.Geom_0\" srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:posList>326257.963 5587646.698 326245.115 5587635.033 326235.925 5587626.372 326224.813 5587615.143 326219.937 5587609.813 326215.43 5587604.582 326069.401 5587429.565 326029.072 5587381.23 326016.953 5587366.245 326006.574 5587352.395 325997.511 5587339.111 325993.426 5587332.613 325989.62 5587326.179 325985.3 5587318.338 325981.095 5587310.098 325976.984 5587301.415 325972.936 5587292.226 325968.752 5587281.917 325965.49 5587272.776 325962.945 5587264.221 325961.068 5587256.077 325956.775 5587234.033 325945.927 5587178.528 325937.046 5587137.054 325935.598 5587128.95 325934.165 5587117.755 325930.94 5587088.165 325930.042 5587076.908 325929.497 5587066.604 325929.259 5587055.713 325929.34 5587043.913 325929.736 5587030.896 325930.504 5587014.723 325935.099 5586927.77 325947.756 5586729.861 325948.594 5586716.762 325949.358 5586707.2 325950.322 5586697.578 325952.855 5586681.431 325954.904 5586669.502 325956.989 5586658.763 325959.167 5586648.983 325961.1 5586641.326 325963.239 5586633.684 325965.581 5586626.065 325968.123 5586618.476 325973.801 5586603.42 325980.246 5586588.586 325986.335 5586576.105 325993.454 5586562.712 326001.738 5586548.115 326013.348 5586528.503 326023.264 5586513.058 326030.759 5586501.831 326037.796 5586491.882 326044.536 5586483.01 326050.54 5586475.664 326057.327 5586467.837 326065.045 5586459.341 326075.553 5586448.131 326086.631 5586436.498 326095.042 5586427.98 326102.836 5586420.503 326110.198 5586413.907 326119.042 5586406.62 326129.025 5586399.037 326140.343 5586390.982 326155.979 5586380.334</gml:posList>\n" +
            "</gml:LineString>\n" +
            "</position>\n" +
            "<bahnkategorie>1100</bahnkategorie>\n" +
            "<elektrifizierung>2000</elektrifizierung>\n" +
            "<anzahlDerStreckengleise>2000</anzahlDerStreckengleise>\n" +
            "<nummerDerBahnstrecke>2631</nummerDerBahnstrecke>\n" +
            "<spurweite>1000</spurweite>\n" +
            "</AX_Bahnstrecke>\n" +
            "</wfs:member>\n" +
            "<wfs:member>\n" +
            "<AX_Bahnstrecke gml:id=\"DENWAT01D0007Mdu\">\n" +
            "<gml:identifier codeSpace=\"http://www.adv-online.de/\">urn:adv:oid:DENWAT01D0007Mdu</gml:identifier>\n" +
            "<lebenszeitintervall>\n" +
            "<AA_Lebenszeitintervall>\n" +
            "<beginnt>2017-10-26T14:06:08Z</beginnt>\n" +
            "</AA_Lebenszeitintervall>\n" +
            "</lebenszeitintervall>\n" +
            "<modellart>\n" +
            "<AA_Modellart>\n" +
            "<advStandardModell>Basis-DLM</advStandardModell>\n" +
            "</AA_Modellart>\n" +
            "</modellart>\n" +
            "<position>\n" +
            "<gml:LineString gml:id=\"o42014.id.316008.position.Geom_0\" srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:posList>328755.685 5589945.851 328739.899 5589909.361 328734.603 5589897.165 328719.388 5589868.249 328713.156 5589856.939 328707.242 5589846.631 328698.083 5589831.567 328688.799 5589817.401 328679.344 5589804.061 328669.688 5589791.505 328661.989 5589782.202 328653.624 5589772.664 328644.486 5589762.769 328634.336 5589752.257 328610.523 5589728.068 328583.072 5589703.263 328549.669 5589677.135 328522.549 5589653.323 328499.067 5589635.133 328467.979 5589610.659 328441.189 5589588.83 328403.231 5589562.36 328335.323 5589506.385</gml:posList>\n" +
            "</gml:LineString>\n" +
            "</position>\n" +
            "<bahnkategorie>1100</bahnkategorie>\n" +
            "<elektrifizierung>2000</elektrifizierung>\n" +
            "<anzahlDerStreckengleise>1000</anzahlDerStreckengleise>\n" +
            "<nummerDerBahnstrecke>2631</nummerDerBahnstrecke>\n" +
            "<spurweite>1000</spurweite>\n" +
            "</AX_Bahnstrecke>\n" +
            "</wfs:member>\n" +
            "<wfs:member>\n" +
            "<AX_Bahnstrecke gml:id=\"DENWAT01D0007KPR\">\n" +
            "<gml:identifier codeSpace=\"http://www.adv-online.de/\">urn:adv:oid:DENWAT01D0007KPR</gml:identifier>\n" +
            "<lebenszeitintervall>\n" +
            "<AA_Lebenszeitintervall>\n" +
            "<beginnt>2015-12-06T19:58:47Z</beginnt>\n" +
            "</AA_Lebenszeitintervall>\n" +
            "</lebenszeitintervall>\n" +
            "<modellart>\n" +
            "<AA_Modellart>\n" +
            "<advStandardModell>Basis-DLM</advStandardModell>\n" +
            "</AA_Modellart>\n" +
            "</modellart>\n" +
            "<position>\n" +
            "<gml:LineString gml:id=\"o42014.id.316009.position.Geom_0\" srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:posList>330219.958 5592609.75 330210.286 5592587.066 330203.712 5592570.32 330200.916 5592562.474 330198.458 5592554.983 330196.309 5592547.752 330194.452 5592540.728 330192.74 5592533.258 330191.233 5592525.53 330189.929 5592517.524 330188.823 5592509.211 330187.912 5592500.581 330187.186 5592491.559 330186.642 5592482.076 330186.27 5592472.007 330185.962 5592442.096 330187.055 5592424.584 330188.132 5592411.82 330189.484 5592400.352 330191.16 5592389.947 330192.563 5592383.031 330194.245 5592375.897 330196.218 5592368.492 330198.508 5592360.722 330204.002 5592344.103 330212.213 5592321.545</gml:posList>\n" +
            "</gml:LineString>\n" +
            "</position>\n" +
            "<bahnkategorie>1100</bahnkategorie>\n" +
            "<elektrifizierung>2000</elektrifizierung>\n" +
            "<anzahlDerStreckengleise>2000</anzahlDerStreckengleise>\n" +
            "<nummerDerBahnstrecke>2631</nummerDerBahnstrecke>\n" +
            "<spurweite>1000</spurweite>\n" +
            "</AX_Bahnstrecke>\n" +
            "</wfs:member>\n" +
            "<wfs:member>\n" +
            "<AX_Bahnstrecke gml:id=\"DENWAT01D0007Js2\">\n" +
            "<gml:identifier codeSpace=\"http://www.adv-online.de/\">urn:adv:oid:DENWAT01D0007Js2</gml:identifier>\n" +
            "<lebenszeitintervall>\n" +
            "<AA_Lebenszeitintervall>\n" +
            "<beginnt>2015-12-06T19:58:47Z</beginnt>\n" +
            "</AA_Lebenszeitintervall>\n" +
            "</lebenszeitintervall>\n" +
            "<modellart>\n" +
            "<AA_Modellart>\n" +
            "<advStandardModell>Basis-DLM</advStandardModell>\n" +
            "</AA_Modellart>\n" +
            "</modellart>\n" +
            "<hatDirektUnten xlink:href=\"urn:adv:oid:DENWAT01D0007Js3\"/>\n" +
            "<position>\n" +
            "<gml:LineString gml:id=\"o42014.id.316010.position.Geom_0\" srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:posList>330212.213 5592321.545 330214.593 5592316.045 330217.799 5592308.71</gml:posList>\n" +
            "</gml:LineString>\n" +
            "</position>\n" +
            "<bahnkategorie>1100</bahnkategorie>\n" +
            "<elektrifizierung>2000</elektrifizierung>\n" +
            "<anzahlDerStreckengleise>2000</anzahlDerStreckengleise>\n" +
            "<nummerDerBahnstrecke>2631</nummerDerBahnstrecke>\n" +
            "<spurweite>1000</spurweite>\n" +
            "</AX_Bahnstrecke>\n" +
            "</wfs:member>\n" +
            "<wfs:member>\n" +
            "<AX_Bahnstrecke gml:id=\"DENWAT01D0007Nie\">\n" +
            "<gml:identifier codeSpace=\"http://www.adv-online.de/\">urn:adv:oid:DENWAT01D0007Nie</gml:identifier>\n" +
            "<lebenszeitintervall>\n" +
            "<AA_Lebenszeitintervall>\n" +
            "<beginnt>2016-03-15T08:36:50Z</beginnt>\n" +
            "</AA_Lebenszeitintervall>\n" +
            "</lebenszeitintervall>\n" +
            "<modellart>\n" +
            "<AA_Modellart>\n" +
            "<advStandardModell>Basis-DLM</advStandardModell>\n" +
            "</AA_Modellart>\n" +
            "</modellart>\n" +
            "<position>\n" +
            "<gml:LineString gml:id=\"o42014.id.316011.position.Geom_0\" srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:posList>326169.547 5584713.697 326188.495 5584746.758 326223.394 5584807.75 326283.141 5584912.168 326309.706 5584962.835 326313.966 5584971.366 326318.091 5584980.457 326322.125 5584990.203 326326.15 5585000.801 326330.039 5585011.875 326333.744 5585023.274 326337.271 5585035.023 326340.631 5585047.153 326344.456 5585062.338 326347.685 5585076.9 326350.348 5585090.968 326352.459 5585104.62 326353.988 5585117.421 326355.121 5585130.515 326355.858 5585143.908 326356.199 5585157.61 326356.131 5585171.852 326355.629 5585185.766 326354.692 5585199.366 326353.318 5585212.669 326348.816 5585249.879 326347.384 5585260.146 326345.733 5585267.717 326344.173 5585272.746 326341.17 5585281.112 326308.001 5585374.222 326303.051 5585389.204 326298.508 5585404.328 326291.387 5585434.887 326288.98 5585446.633 326287.042 5585457.432 326284.609 5585474.604 326282.908 5585492.223 326281.942 5585510.264 326281.713 5585528.702 326281.914 5585541.103 326283.646 5585565.753 326284.575 5585575.233 326285.643 5585583.886 326287.816 5585597.401 326290.73 5585611.737 326294.443 5585627.207 326299.214 5585644.886 326304.209 5585661.821 326308.843 5585675.621 326313.667 5585687.924 326318.814 5585699.037 326350.907 5585762.691 326367.002 5585794.466 326370.934 5585802.565 326374.724 5585811.057 326378.393 5585819.989 326381.972 5585829.437 326385.451 5585839.356 326388.944 5585850.06 326396.492 5585875.439 326401.516 5585893.674 326405.422 5585909.171 326408.585 5585923.427 326411.079 5585936.798 326413.037 5585950.11 326414.447 5585963.364 326415.305 5585976.523 326415.611 5585989.556 326415.457 5586000 326414.956 5586010.499 326414.033 5586021.968 326412.697 5586033.876 326410.937 5586046.322 326408.728 5586059.488 326405.307 5586076.952 326401.522 5586093.286 326397.327 5586108.693 326392.69 5586123.284 326388.41 5586135.1 326383.317 5586147.898 326377.346 5586161.899 326368.62 5586181.5</gml:posList>\n" +
            "</gml:LineString>\n" +
            "</position>\n" +
            "<bahnkategorie>1100</bahnkategorie>\n" +
            "<elektrifizierung>2000</elektrifizierung>\n" +
            "<anzahlDerStreckengleise>2000</anzahlDerStreckengleise>\n" +
            "<nummerDerBahnstrecke>2631</nummerDerBahnstrecke>\n" +
            "<spurweite>1000</spurweite>\n" +
            "</AX_Bahnstrecke>\n" +
            "</wfs:member>\n" +
            "<wfs:member>\n" +
            "<AX_Bahnstrecke gml:id=\"DENWAT01D0007JAL\">\n" +
            "<gml:identifier codeSpace=\"http://www.adv-online.de/\">urn:adv:oid:DENWAT01D0007JAL</gml:identifier>\n" +
            "<lebenszeitintervall>\n" +
            "<AA_Lebenszeitintervall>\n" +
            "<beginnt>2015-12-06T19:58:47Z</beginnt>\n" +
            "</AA_Lebenszeitintervall>\n" +
            "</lebenszeitintervall>\n" +
            "<modellart>\n" +
            "<AA_Modellart>\n" +
            "<advStandardModell>Basis-DLM</advStandardModell>\n" +
            "</AA_Modellart>\n" +
            "</modellart>\n" +
            "<position>\n" +
            "<gml:LineString gml:id=\"o42014.id.316012.position.Geom_0\" srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:posList>330227.519 5592625.957 330237.769 5592642.099 330245.49 5592653.826 330252.732 5592664.251 330259.667 5592673.595 330264.728 5592679.987 330269.803 5592686.032 330274.907 5592691.749 330280.051 5592697.148 330285.248 5592702.244 330290.501 5592707.039 330295.817 5592711.539 330301.201 5592715.749 330352.081 5592753.923 330360.23 5592760.203 330380.609 5592776.388 330382.672 5592777.981 330391.274 5592784.579 330399.901 5592791.179 330413.172 5592801.461 330423.798 5592809.969 330432.492 5592817.234 330440.226 5592824.042 330447.267 5592830.625 330453.716 5592837.082 330457.123 5592840.694 330463.036 5592847.297 330469.08 5592854.466 330475.355 5592862.316 330482.113 5592871.161 330490.216 5592882.21 330497.63 5592892.823 330504.452 5592903.139 330510.734 5592913.236 330516.249 5592922.705 330521.301 5592932.024 330526.901 5592943.319 330531.813 5592954.416 330535.629 5592964.167 330539.002 5592973.982 330542.017 5592984.026 330544.54 5592993.528 330546.831 5593003.286 330548.891 5593013.309 330550.722 5593023.605 330552.669 5593036.663 330554.352 5593050.606 330555.808 5593065.705 330557.117 5593082.791 330560.599 5593140.697 330562.624 5593167.032 330564.437 5593184.643 330566.641 5593202.029 330569.236 5593219.182 330572.22 5593236.096 330575.579 5593252.854 330580.526 5593275.691 330584.904 5593292.672 330589.7 5593309.546 330592.697 5593319.332 330596.511 5593330.448 330600.577 5593342.212 330608.932 5593365.766 330616.397 5593385.725 330623.633 5593403.846 330630.756 5593420.379 330636.585 5593432.937 330642.503 5593444.815 330650.751 5593460.08 330660.313 5593476.46 330671.364 5593494.307 330686.912 5593518.477 330692.135 5593526.372 330822.509 5593723.855 330832.671 5593739.569 330833.247 5593740.462 330837.645 5593747.265 330865.956 5593791.051 330887.521 5593824.436 330944.955 5593913.352 330950.098 5593921.751 330963.816 5593944.213 330990.807 5593988.26 330998.939 5594001.908 331006.077 5594014.259 331013.887 5594028.306 331021.89 5594043.278 331030.207 5594059.401 331039.147 5594077.271 331071.847 5594143.5 331102.157 5594210.025 331102.709 5594211.304 331111.474 5594231.21 331121.392 5594252.966 331165.76 5594346.869 331176.003 5594369.087 331184.953 5594389.115 331192.509 5594406.632 331199.907 5594424.388 331207.148 5594442.385 331214.233 5594460.623 331221.162 5594479.106 331227.937 5594497.837 331234.56 5594516.821 331241.031 5594536.061 331248.72 5594559.942 331255.443 5594582.118 331261.37 5594603.147 331266.574 5594623.294 331269.887 5594637.238 331273.374 5594652.845 331287.101 5594717.636 331297.431 5594763.024 331301.726 5594782.873 331303.759 5594793.019 331306.317 5594806.91 331308.868 5594822.139 331314.851 5594862.158 331339.542 5595032.053 331340.788 5595040.609 331344.162 5595060.053 331347.049 5595074.181 331350.062 5595086.366 331353.65 5595098.792 331357.823 5595111.485 331362.594 5595124.491 331364.561 5595129.504 331369.51 5595141.286 331374.551 5595152.095 331379.768 5595162.108 331385.206 5595171.413 331390.356 5595179.326 331396.292 5595187.699 331403.158 5595196.755 331412.21 5595208.15 331422.113 5595220.255 331430.017 5595229.352 331437.476 5595237.227 331444.686 5595244.065 331449.913 5595248.541 331455.931 5595253.332 331480.015 5595271.358 331482.585 5595273.055 331482.789 5595273.147 331504.318 5595287.034 331520.371 5595296.747 331527.357 5595300.684 331533.801 5595304.094 331539.923 5595307.095 331545.814 5595309.729 331553.866 5595312.934 331571.805 5595319.43 331579.654 5595322.471 331591.898 5595327.951 331607.312 5595335.764 331614.541 5595339.59 331627.017 5595346.629 331637.953 5595353.539 331644.823 5595358.36 331651.564 5595363.494 331658.168 5595368.935 331664.627 5595374.677 331666.914 5595376.787 331679.606 5595388.924 331690.791 5595400.54 331696.973 5595407.395 331706.419 5595418.562 331713.171 5595426.855 331718.844 5595434.208 331724.293 5595441.674 331729.514 5595449.246 331734.502 5595456.916 331741.082 5595467.802 331747.202 5595478.874 331752.85 5595490.111 331758.02 5595501.499 331761.997 5595510.746 331766.829 5595522.878 331770.155 5595531.832 331772.88 5595539.828</gml:posList>\n" +
            "</gml:LineString>\n" +
            "</position>\n" +
            "<bahnkategorie>1100</bahnkategorie>\n" +
            "<elektrifizierung>2000</elektrifizierung>\n" +
            "<anzahlDerStreckengleise>2000</anzahlDerStreckengleise>\n" +
            "<nummerDerBahnstrecke>2631</nummerDerBahnstrecke>\n" +
            "<spurweite>1000</spurweite>\n" +
            "</AX_Bahnstrecke>\n" +
            "</wfs:member>\n" +
            "<wfs:member>\n" +
            "<AX_Bahnstrecke gml:id=\"DENWAT01TN000003\">\n" +
            "<gml:identifier codeSpace=\"http://www.adv-online.de/\">urn:adv:oid:DENWAT01TN000003</gml:identifier>\n" +
            "<lebenszeitintervall>\n" +
            "<AA_Lebenszeitintervall>\n" +
            "<beginnt>2016-03-15T08:36:50Z</beginnt>\n" +
            "</AA_Lebenszeitintervall>\n" +
            "</lebenszeitintervall>\n" +
            "<modellart>\n" +
            "<AA_Modellart>\n" +
            "<advStandardModell>Basis-DLM</advStandardModell>\n" +
            "</AA_Modellart>\n" +
            "</modellart>\n" +
            "<hatDirektUnten xlink:href=\"urn:adv:oid:DENWAT01D000AAPY\"/>\n" +
            "<position>\n" +
            "<gml:LineString gml:id=\"o42014.id.316013.position.Geom_0\" srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:posList>334365.596 5590107.303 334346.981 5590112.735 334341.344 5590114.38 334340.681 5590114.573 334340.098 5590114.743 334338.852 5590115.105 334337.27 5590115.564 334336.61 5590115.755 334317.331 5590121.192 334294.892 5590127.463 334287.141 5590129.958 334280.226 5590132.482 334273.551 5590135.252 334266.257 5590138.604 334244.555 5590149.554 334214.575 5590164.34 334202.485 5590170.514 334199.4 5590172.152 334177.273 5590184.525 334155.263 5590197.162 334138.475 5590206.495 334121.689 5590215.827 334114.783 5590219.728 334108.738 5590223.142 334083.333 5590237.491</gml:posList>\n" +
            "</gml:LineString>\n" +
            "</position>\n" +
            "<bahnkategorie>1100</bahnkategorie>\n" +
            "<elektrifizierung>2000</elektrifizierung>\n" +
            "<anzahlDerStreckengleise>1000</anzahlDerStreckengleise>\n" +
            "<spurweite>1000</spurweite>\n" +
            "<zustand>2100</zustand>\n" +
            "</AX_Bahnstrecke>\n" +
            "</wfs:member>\n" +
            "<wfs:member>\n" +
            "<AX_Bahnstrecke gml:id=\"DENWAT01D0007Mee\">\n" +
            "<gml:identifier codeSpace=\"http://www.adv-online.de/\">urn:adv:oid:DENWAT01D0007Mee</gml:identifier>\n" +
            "<lebenszeitintervall>\n" +
            "<AA_Lebenszeitintervall>\n" +
            "<beginnt>2015-12-06T19:58:47Z</beginnt>\n" +
            "</AA_Lebenszeitintervall>\n" +
            "</lebenszeitintervall>\n" +
            "<modellart>\n" +
            "<AA_Modellart>\n" +
            "<advStandardModell>Basis-DLM</advStandardModell>\n" +
            "</AA_Modellart>\n" +
            "</modellart>\n" +
            "<position>\n" +
            "<gml:LineString gml:id=\"o42014.id.316014.position.Geom_0\" srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:posList>328327.775 5589500.385 328301.805 5589479.317 328220.991 5589413.629 328215.082 5589409.029 328208.298 5589404.134 328197.68 5589397.001 328186.18 5589389.833 328173.596 5589382.503 328159.372 5589374.686 328032.325 5589306.562 328027.796 5589303.973 328022.247 5589300.484 328012.309 5589294.161 328006.053 5589290.41 327997.321 5589285.199 327988.711 5589279.802 327805.651 5589160.4 327794.847 5589153.374 327787.778 5589149.013 327767.087 5589137.239 327486.953 5588968.316 327482.644 5588965.688 327478.023 5588962.772 327424.715 5588931.4 327368.881 5588896.927 327346.594 5588882.816 327329.485 5588872.999 327297.515 5588849.571 327285.285 5588839.157 327269.729 5588825.674 327256.247 5588813.848 327243.262 5588802.224 327137.835 5588707.051 327124.223 5588694.762 327120.598 5588691.49 327109.661 5588682.015 327088.59 5588664.661 327078.844 5588656.411 327076.257 5588654.19 327073.692 5588651.982 327068.711 5588647.686 327053.166 5588634.455 327044.862 5588627.671 327037.217 5588621.702 327022.349 5588610.393 326944.071 5588550.506 326935.905 5588543.928 326927.449 5588536.459 326918.492 5588527.921 326908.108 5588517.432 326894.805 5588503.399 326883.456 5588490.707 326875.734 5588481.511 326862.871 5588465.398 326857.77 5588458.522 326853.225 5588451.997 326848.742 5588445.073 326837.828 5588427.6 326831.206 5588417.259 326826.661 5588409.217 326821.451 5588398.738 326811.503 5588377.924 326800.614 5588351.143 326795.995 5588339.013 326791.919 5588326.675 326789.268 5588317.494 326786.761 5588307.754 326784.407 5588297.518 326782.332 5588287.531 326776.301 5588257.151</gml:posList>\n" +
            "</gml:LineString>\n" +
            "</position>\n" +
            "<bahnkategorie>1100</bahnkategorie>\n" +
            "<elektrifizierung>2000</elektrifizierung>\n" +
            "<anzahlDerStreckengleise>1000</anzahlDerStreckengleise>\n" +
            "<nummerDerBahnstrecke>2631</nummerDerBahnstrecke>\n" +
            "<spurweite>1000</spurweite>\n" +
            "</AX_Bahnstrecke>\n" +
            "</wfs:member>\n" +
            "<wfs:member>\n" +
            "<AX_Bahnstrecke gml:id=\"DENWAT01D0007LJK\">\n" +
            "<gml:identifier codeSpace=\"http://www.adv-online.de/\">urn:adv:oid:DENWAT01D0007LJK</gml:identifier>\n" +
            "<lebenszeitintervall>\n" +
            "<AA_Lebenszeitintervall>\n" +
            "<beginnt>2015-12-06T19:58:47Z</beginnt>\n" +
            "</AA_Lebenszeitintervall>\n" +
            "</lebenszeitintervall>\n" +
            "<modellart>\n" +
            "<AA_Modellart>\n" +
            "<advStandardModell>Basis-DLM</advStandardModell>\n" +
            "</AA_Modellart>\n" +
            "</modellart>\n" +
            "<hatDirektUnten xlink:href=\"urn:adv:oid:DENWAT01D0007LJL\"/>\n" +
            "<position>\n" +
            "<gml:LineString gml:id=\"o42014.id.316015.position.Geom_0\" srsName=\"urn:ogc:def:crs:EPSG::25832\" srsDimension=\"2\">\n" +
            "<gml:posList>326155.979 5586380.334 326159.299 5586378.298 326162.816 5586376.155</gml:posList>\n" +
            "</gml:LineString>\n" +
            "</position>\n" +
            "<bahnkategorie>1100</bahnkategorie>\n" +
            "<elektrifizierung>2000</elektrifizierung>\n" +
            "<anzahlDerStreckengleise>2000</anzahlDerStreckengleise>\n" +
            "<nummerDerBahnstrecke>2631</nummerDerBahnstrecke>\n" +
            "<spurweite>1000</spurweite>\n" +
            "</AX_Bahnstrecke>\n" +
            "</wfs:member>\n" +
            "</wfs:FeatureCollection>";
}