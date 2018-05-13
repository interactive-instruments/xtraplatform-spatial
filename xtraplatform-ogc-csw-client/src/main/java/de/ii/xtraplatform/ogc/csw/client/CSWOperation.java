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

import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public abstract class CSWOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSWOperation.class);

    public CSWOperation() {

    }

    protected void initialize(XMLNamespaceNormalizer nsStore) {};

    public abstract CSW.OPERATION getOperation();

    protected String getOperationName(CSW.VERSION version) {
        return CSW.getWord(version, getOperation());
    }

    public Map<String, String> getRequestHeaders(){
        return new HashMap<>();
    }
    
    public void setResponseHeaders(Header[] headers){
        // nothing to do
    }

    protected abstract Element toXml(XMLDocument document, Element operationElement, CSW.VERSION version);

    protected abstract Map<String, String> toKvp(Map<String, String> parameters, XMLNamespaceNormalizer nsStore, CSW.VERSION version);

    // returns the XML POST Request as String
    public String toXml(XMLNamespaceNormalizer nsStore, CSW.VERSION version) {
        this.initialize(nsStore);

        XMLDocument document = new XMLDocument(nsStore);

        Element operationElement = document.createElementNS(CSW.getNS(version), CSW.getPR(version), getOperationName(version));


        for (String uri : nsStore.xgetNamespaceUris()) {
            operationElement.setAttribute("xmlns:" + uri, nsStore.getNamespaceURI(uri));
        }

        operationElement.setAttribute(CSW.getWord(version, CSW.VOCABULARY.SERVICE), CSW.getWord(version, CSW.VOCABULARY.CSW));

        if( version != null) {
            operationElement.setAttribute(CSW.getWord(version, CSW.VOCABULARY.VERSION), version.toString());
        }

        operationElement = toXml(document, operationElement, version);
        document.appendChild(operationElement);

        String out;

        if (LOGGER.isDebugEnabled()) {
            out = document.toString(true);
            LOGGER.debug(out);
        } else {
            out = document.toString(false);
        }

        return out;
    }

    // Returns the KVP parameters for a GET request as Map<String, String>
    public Map<String, String> toKvp(XMLNamespaceNormalizer nsStore, CSW.VERSION version) {

        Map<String, String> parameters = new HashMap<>();

        //if (version == null)
        //    version = CSW.VERSION._2_0_0;

        parameters.put(CSW.getWord(version, CSW.VOCABULARY.SERVICE).toUpperCase(), CSW.getWord(version, CSW.VOCABULARY.CSW));
        parameters.put(CSW.getWord(version, CSW.VOCABULARY.REQUEST).toUpperCase(), getOperationName(version));

        if( version != null) {
            parameters.put(CSW.getWord(version, CSW.VOCABULARY.VERSION).toUpperCase(), version.toString());
        }

        parameters = toKvp(parameters, nsStore, version);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(parameters.toString());
        }

        return parameters;
    }
}
