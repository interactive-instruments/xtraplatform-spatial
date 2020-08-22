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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.ii.xtraplatform.ogc.csw.client;

import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.xml.domain.XMLDocument;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;
import org.w3c.dom.Element;

import java.util.Map;

/**
 *
 * @author fischer
 */
public class CSWOperationGetCapabilities extends CSWOperation {

    public CSWOperationGetCapabilities() {

    }

    @Override
    public CSW.OPERATION getOperation() {
        return CSW.OPERATION.GET_CAPABILITES;
    }

    @Override
    protected Element toXml(XMLDocument document, Element operationElement, CSW.VERSION version) {
        return operationElement;
    }

    @Override
    protected Map<String, String> toKvp(Map<String, String> parameters, XMLNamespaceNormalizer nsStore, CSW.VERSION version) {
        return parameters;
    }
}
