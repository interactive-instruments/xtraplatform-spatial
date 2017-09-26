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
package de.ii.xtraplatform.ogc.csw.client;

import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Map;

/**
 *
 * @author fischer
 */
public class CSWOperationGetRecords extends CSWOperationQuery {
    private String constraintLanguage;
    private String constraintLanguageVersion;
    private String elementSetName;
    private String outputSchema;
    private String constraint;

    public CSWOperationGetRecords() {
        this.constraintLanguage = "CQL_TEXT";
        this.constraintLanguageVersion = "1.1.0";
        this.elementSetName = "full";
        this.outputSchema = "http://www.isotc211.org/2005/gmd";
    }

    public void setConstraintLanguage(String constraintLanguage) {
        this.constraintLanguage = constraintLanguage;
    }

    public void setElementSetName(String elementSetName) {
        this.elementSetName = elementSetName;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }

    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }

    @Override
    public CSW.OPERATION getOperation() {
        return CSW.OPERATION.GET_RECORDS;
    }

    @Override
    protected Element toXml(XMLDocument document, Element operationElement, CSW.VERSION version) {

        operationElement = super.toXml(document, operationElement, version);

        if (this.outputSchema != null) {
            operationElement.setAttribute(CSW.getWord(version, CSW.VOCABULARY.OUPUTSCHEMA), String.valueOf(outputSchema));
        }

        if (this.elementSetName != null) {
            Element setName = document.createElementNS(CSW.getNS(version), CSW.getPR(version), CSW.getWord(version, CSW.VOCABULARY.ELEMENTSETNAME));
            setName.setTextContent(elementSetName);

            Node query = operationElement.getFirstChild();
            query.insertBefore(setName, query.getFirstChild());
        }

        return operationElement;
    }

    @Override
    protected Map<String, String> toKvp(Map<String, String> parameters, XMLNamespaceNormalizer nsStore, CSW.VERSION version) {

        if (this.constraintLanguage != null) {
            parameters.put(CSW.getWord(version, CSW.VOCABULARY.CONSTRAINTLANGUAGE).toUpperCase(), String.valueOf(constraintLanguage));
        }

        if (this.constraintLanguageVersion != null) {
            parameters.put(CSW.getWord(version, CSW.VOCABULARY.CONSTRAINT_LANGUAGE_VERSION).toUpperCase(), String.valueOf(constraintLanguageVersion));
        }

        if (this.elementSetName != null) {
            parameters.put(CSW.getWord(version, CSW.VOCABULARY.ELEMENTSETNAME).toUpperCase(), String.valueOf(elementSetName));
        }

        if (this.outputSchema != null) {
            parameters.put(CSW.getWord(version, CSW.VOCABULARY.OUPUTSCHEMA).toUpperCase(), String.valueOf(outputSchema));
        }

        if (this.constraint != null) {
            parameters.put(CSW.getWord(version, CSW.VOCABULARY.CONSTRAINT).toUpperCase(), String.valueOf(constraint));
        }

        return super.toKvp(parameters, nsStore, version);
    }
}
