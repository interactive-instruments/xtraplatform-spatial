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

import com.google.common.base.Joiner;
import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public abstract class CSWOperationQuery extends  CSWOperation {

    private List<CSWQuery> queries;
    private Integer maxRecords;
    private Integer startPosition;
    private boolean hits;
    private CSW.VOCABULARY resultType;

    public CSWOperationQuery() {
        this.queries = new ArrayList<>();
    }

    public void addQuery(CSWQuery query) {
        this.queries.add(query);
    }

    public void setMaxRecords(int maxRecords) {
        this.maxRecords = maxRecords;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public void setHits() {
        this.hits = true;
    }

    public void setResultType(CSW.VOCABULARY resultType) {
        this.resultType = resultType;
    }

    @Override
    protected Element toXml(XMLDocument document, Element operationElement, CSW.VERSION version) {

        if (this.maxRecords != null) {
            operationElement.setAttribute(CSW.getWord(version, CSW.VOCABULARY.MAXRECORDS), String.valueOf(maxRecords));
        }

        if (this.startPosition != null) {
            operationElement.setAttribute(CSW.getWord(version, CSW.VOCABULARY.STARTPOSITION), String.valueOf(startPosition));
        }

        if (this.resultType != null) {
            operationElement.setAttribute(CSW.getWord(version, CSW.VOCABULARY.RESULTTYPE), CSW.getWord(version, resultType));
        }

        for (CSWQuery query : queries) {
            Element queryElement = query.toXML(document, version);
            operationElement.appendChild(queryElement);
        }

        return operationElement;
    }

    @Override
    protected Map<String, String> toKvp(Map<String, String> parameters, XMLNamespaceNormalizer nsStore, CSW.VERSION version) {

        if (this.maxRecords != null) {
            parameters.put(CSW.getWord(version, CSW.VOCABULARY.MAXRECORDS).toUpperCase(), String.valueOf(maxRecords));
        }

        if (this.startPosition != null) {
            parameters.put(CSW.getWord(version, CSW.VOCABULARY.STARTPOSITION).toUpperCase(), String.valueOf(startPosition));
        }

        if (this.resultType != null) {
            parameters.put(CSW.getWord(version, CSW.VOCABULARY.RESULTTYPE).toUpperCase(), CSW.getWord(version, resultType));
        }

        for (CSWQuery query : queries) {
            parameters = query.toKVP(parameters, nsStore, version);
        }

        if (!nsStore.getNamespaces().isEmpty()) {
            StringBuilder ns = new StringBuilder("xmlns(");
            Joiner.on("),").withKeyValueSeparator("=").appendTo(ns, nsStore.getNamespaces());
            ns.append(")");

            parameters.put(CSW.getWord(version, CSW.VOCABULARY.NAMESPACES).toUpperCase(), ns.toString());
        }
        return parameters;
    }
}
