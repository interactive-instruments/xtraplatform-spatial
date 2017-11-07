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
import de.ii.xtraplatform.ogc.api.WFS;
import org.codehaus.staxmate.in.SMInputCursor;

import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zahnen
 */
public class ExtractWfsUrlsCSWRecordsAnalyzer extends AbstractCSWRecordsAnalyzer {

    private Set<String> urls;
    private boolean currentServiceIsWfs;
    private boolean currentOperationIsGetCapabilities;
    private int numberMatched;
    private int numberReturned;
    private int numberParsed;
    private int nextRecord;
    private boolean started;

    public ExtractWfsUrlsCSWRecordsAnalyzer() {
        this.urls = new LinkedHashSet<>();
        this.numberMatched = -1;
        this.numberReturned = -1;
        this.numberParsed = 0;
        this.nextRecord = -1;
    }

    @Override
    public void analyzeStart(SMInputCursor searchResults) {
        this.started = true;
        try {
            this.numberMatched = Integer.parseInt(searchResults.getAttrValue(CSW.getWord(CSW.VOCABULARY.NUMBER_OF_RECORDS_MATCHED)));
            this.numberReturned = Integer.parseInt(searchResults.getAttrValue(CSW.getWord(CSW.VOCABULARY.NUMBER_OF_RECORDS_RETURNED)));
            this.nextRecord = Integer.parseInt(searchResults.getAttrValue(CSW.getWord(CSW.VOCABULARY.NEXT_RECORD)));
        } catch (XMLStreamException | NumberFormatException e) {
            e.printStackTrace();
            this.nextRecord = -1;
        }
    }

    @Override
    public void analyzeEnd() {
        this.numberParsed += Math.max(0, numberReturned);
        //this.numberReturned = -1;
    }

    @Override
    public void analyzeRecordStart() {
        this.currentServiceIsWfs = false;
        this.currentOperationIsGetCapabilities = false;
    }

    @Override
    public void analyzeRecordEnd() {

    }

    @Override
    public void analyzeServiceType(String serviceType) {

    }

    @Override
    public void analyzeServiceTypeVersion(String serviceTypeVersion) {
        if (serviceTypeVersion.toUpperCase().contains("WFS")) {
            this.currentServiceIsWfs = true;
        }
    }

    @Override
    public void analyzeOperationName(String operationName) {
        if (operationName.toLowerCase().equals("getcapabilities")) {
            this.currentOperationIsGetCapabilities = true;
        } else {
            this.currentOperationIsGetCapabilities = false;
        }
    }

    @Override
    public void analyzeOperationUrl(String operationUrl) {
        if (currentOperationIsGetCapabilities) {
            if (currentServiceIsWfs || (operationUrl.toLowerCase().contains("service=wfs") && operationUrl.toLowerCase().contains("request=getcapabilities")))
            this.urls.add(WFS.cleanUrl(operationUrl));
        }
    }

    public Collection<String> getUrls() {
        return urls;
    }

    public int getNumberMatched() {
        return numberMatched;
    }

    public int getNumberReturned() {
        return numberReturned;
    }

    public int getNextRecord() {
        return nextRecord;
    }

    public int getNumberParsed() {
        return numberParsed;
    }

    public boolean hasMore() {
       return  !started || (nextRecord > 0 && numberMatched > 0 && numberReturned > 0 && numberParsed < numberMatched);
    }
}
