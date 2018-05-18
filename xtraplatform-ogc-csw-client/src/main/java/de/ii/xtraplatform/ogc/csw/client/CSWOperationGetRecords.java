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
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
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
            Element setName = document.createElementNS(CSW.getNS(version), CSW.getWord(version, CSW.VOCABULARY.ELEMENTSETNAME));
            setName.setTextContent(elementSetName);

            Node query = operationElement.getFirstChild();
            query.insertBefore(setName, query.getFirstChild());
        }

        return operationElement;
    }

    @Override
    protected Map<String, String> toKvp(Map<String, String> parameters, XMLNamespaceNormalizer nsStore, CSW.VERSION version) throws ParserConfigurationException {

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
