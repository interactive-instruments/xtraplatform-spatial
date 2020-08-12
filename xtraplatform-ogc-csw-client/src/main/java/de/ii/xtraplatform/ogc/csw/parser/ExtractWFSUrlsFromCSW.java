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
package de.ii.xtraplatform.ogc.csw.parser;

import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.ogc.api.exceptions.ReadError;
import de.ii.xtraplatform.ogc.api.filter.OGCFilter;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterAnd;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterLiteral;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterPropertyIsEqualTo;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterPropertyIsLike;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterValueReference;
import de.ii.xtraplatform.ogc.parser.LoggingWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.parser.MultiWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.parser.WFSCapabilitiesParser;
import de.ii.xtraplatform.ogc.csw.client.CSWAdapter;
import de.ii.xtraplatform.ogc.csw.client.CSWOperation;
import de.ii.xtraplatform.ogc.csw.client.CSWOperationGetCapabilities;
import de.ii.xtraplatform.ogc.csw.client.CSWOperationGetRecords;
import de.ii.xtraplatform.ogc.csw.client.CSWQuery;
import de.ii.xtraplatform.ogc.csw.client.CSWRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.staxmate.SMInputFactory;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * @author zahnen
 */
public class ExtractWFSUrlsFromCSW {

    private HttpClient httpClient;
    private SMInputFactory staxFactory;


    public ExtractWFSUrlsFromCSW(HttpClient httpClient, SMInputFactory staxFactory) {
        this.httpClient = httpClient;
        this.staxFactory = staxFactory;

    }

    public CSWCapabilitiesAnalyzer caps(String url) {
        CSWAdapter adapter = new CSWAdapter(url);
        adapter.initialize(httpClient, httpClient);

        CSWOperation getCapabilities = new CSWOperationGetCapabilities();
        CSWCapabilitiesAnalyzer capabilitiesAnalyzer = new CSWCapabilitiesAnalyzer();
        LoggingWfsCapabilitiesAnalyzer capabilitiesAnalyzer1 = new LoggingWfsCapabilitiesAnalyzer();
        MultiWfsCapabilitiesAnalyzer capabilitiesAnalyzer2 = new MultiWfsCapabilitiesAnalyzer(capabilitiesAnalyzer, capabilitiesAnalyzer1);
        WFSCapabilitiesParser wfsCapabilitiesParser = new WFSCapabilitiesParser(capabilitiesAnalyzer, staxFactory);


        CSWRequest request = new CSWRequest(adapter, getCapabilities);




        HttpEntity entity = null;

            try {
                entity = request.getResponse().get();

            /*String readLine;
            BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));

            while (((readLine = br.readLine()) != null)) {
                System.out.println(readLine);


            }*/

                wfsCapabilitiesParser.parse(entity);

                System.out.println();
                System.out.println();
                System.out.println(capabilitiesAnalyzer.title);
                System.out.println(capabilitiesAnalyzer.version);
                System.out.println(capabilitiesAnalyzer.url);
                System.out.println(capabilitiesAnalyzer.outputFormats.toString());
                System.out.println(capabilitiesAnalyzer.typeNames.toString());
                System.out.println(capabilitiesAnalyzer.constraintLanguages.toString());
                System.out.println(capabilitiesAnalyzer.isoQueryables.toString());
                System.out.println();


            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } finally {
                EntityUtils.consumeQuietly(entity);
            }


            return capabilitiesAnalyzer;
    }

    public Collection<String> extract(String url) {
        CSWCapabilitiesAnalyzer capabilitiesAnalyzer = caps(url);


        CSWAdapter adapter = new CSWAdapter(url);
        adapter.initialize(httpClient, httpClient);

        CSW.VERSION version = CSW.VERSION.fromString("2.0.2");
        adapter.setVersion(version.toString());
        adapter.getNsStore().addNamespace(CSW.getPR(version), CSW.getNS(version));

        CSWOperationGetRecords getRecords = createGetRecords(capabilitiesAnalyzer);

        //getRecords.toKvp(adapter.getNsStore(), version);
        //getRecords.toXml(adapter.getNsStore(), version);

        //adapter.setHttpMethod("POST");

        CSWRequest request = new CSWRequest(adapter, getRecords);

        ExtractWfsUrlsCSWRecordsAnalyzer analyzer = new ExtractWfsUrlsCSWRecordsAnalyzer();
        LoggingCSWRecordsAnalyzer loggingCSWRecordsAnalyzer = new LoggingCSWRecordsAnalyzer();
        MultiCSWRecordsAnalyzer multiCSWRecordsAnalyzer = new MultiCSWRecordsAnalyzer(loggingCSWRecordsAnalyzer, analyzer);


        CSWRecordsParser recordParser = new CSWRecordsParser(multiCSWRecordsAnalyzer, staxFactory);

        HttpEntity entity = null;
        int nextRecord = 1;

        while (nextRecord > 0 && analyzer.hasMore()) {
            getRecords.setStartPosition(nextRecord);

            try {
                entity = request.getResponse().get();

                recordParser.parse(entity);

                // TODO: loop over nextRecord

                System.out.println();
                System.out.println();
                System.out.println(analyzer.getNumberParsed() + " records of " + analyzer.getNumberMatched() + " parsed");
                System.out.println(analyzer.getUrls().size() + " WFS found:");
                System.out.println(analyzer.getUrls().toString());
                System.out.println();

                nextRecord = analyzer.getNextRecord();


            } catch (InterruptedException | ExecutionException e) {
                // ignore
                //e.printStackTrace();
                if (e.getCause().getClass() == ReadError.class)  {
                    throw new IllegalArgumentException(e.getCause().getMessage());
                }
            } finally {
                EntityUtils.consumeQuietly(entity);
            }
        }

        return analyzer.getUrls();
    }

    private CSWOperationGetRecords createGetRecords(CSWCapabilitiesAnalyzer capabilitiesAnalyzer) {
        CSWOperationGetRecords getRecords = new CSWOperationGetRecords();
        getRecords.setMaxRecords(100);
        getRecords.setStartPosition(1);
        getRecords.setResultType(CSW.VOCABULARY.HITS);
        getRecords.setResultType(CSW.VOCABULARY.RESULTS);

        CSWQuery query = new CSWQuery();
        query.addTypename("gmd:MD_Metadata");
        getRecords.addQuery(query);

        if (capabilitiesAnalyzer.constraintLanguages.contains("FILTER")) {
            getRecords.setConstraintLanguage("FILTER");

            OGCFilter filter = new OGCFilter();
            OGCFilterAnd and = new OGCFilterAnd();
            filter.addExpression(and);

            if (capabilitiesAnalyzer.isoQueryables.containsKey("Type")) {
                OGCFilterValueReference valueReference = new OGCFilterValueReference(capabilitiesAnalyzer.isoQueryables.get("Type"));
                OGCFilterLiteral literal = new OGCFilterLiteral("service");
                OGCFilterPropertyIsEqualTo isEqualTo = new OGCFilterPropertyIsEqualTo(valueReference, literal);
                and.addOperand(isEqualTo);
            }
            if (capabilitiesAnalyzer.isoQueryables.containsKey("Type") && capabilitiesAnalyzer.isoQueryables.containsKey("serviceType")) {
                OGCFilterValueReference valueReference = new OGCFilterValueReference(capabilitiesAnalyzer.isoQueryables.get("serviceType"));
                OGCFilterLiteral literal = new OGCFilterLiteral("%download%");
                OGCFilterPropertyIsLike isLike = new OGCFilterPropertyIsLike(valueReference, literal);
                and.addOperand(isLike);
            }
            /*if (capabilitiesAnalyzer.isoQueryables.containsKey("Type") && capabilitiesAnalyzer.isoQueryables.containsKey("serviceType") && capabilitiesAnalyzer.isoQueryables.containsKey("serviceTypeVersion")) {
                OGCFilterValueReference valueReference = new OGCFilterValueReference(capabilitiesAnalyzer.isoQueryables.get("serviceTypeVersion"));
                OGCFilterLiteral literal = new OGCFilterLiteral("%WFS%");
                OGCFilterPropertyIsLike isLike = new OGCFilterPropertyIsLike(valueReference, literal);
                and.addOperand(isLike);
            }*/

            query.addFilter(filter);

        } else if (capabilitiesAnalyzer.constraintLanguages.contains("CQL_TEXT")) {
            getRecords.setConstraintLanguage("CQL_TEXT");

            StringBuilder constraint = new StringBuilder();

            if (capabilitiesAnalyzer.isoQueryables.containsKey("Type")) {
                constraint.append(capabilitiesAnalyzer.isoQueryables.get("Type"));
                constraint.append(" = 'service'");
            }
            if (capabilitiesAnalyzer.isoQueryables.containsKey("Type") && capabilitiesAnalyzer.isoQueryables.containsKey("serviceType")) {
                constraint.append(" AND ");
                constraint.append(capabilitiesAnalyzer.isoQueryables.get("serviceType"));
                constraint.append(" LIKE '%download%'");
            }
            /*if (capabilitiesAnalyzer.isoQueryables.containsKey("Type") && capabilitiesAnalyzer.isoQueryables.containsKey("serviceType") && capabilitiesAnalyzer.isoQueryables.containsKey("serviceTypeVersion")) {
                constraint.append(" AND ");
                constraint.append(capabilitiesAnalyzer.isoQueryables.get("serviceTypeVersion"));
                constraint.append(" LIKE '%WFS%'");
            }*/

            getRecords.setConstraint(constraint.toString());

            //getRecords.setConstraint("csw:AnyText LIKE '%WFS%'");
        }


        return getRecords;
    }
}
