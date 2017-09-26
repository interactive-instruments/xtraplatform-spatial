/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.ii.xtraplatform.ogc.csw.client;

import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
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
