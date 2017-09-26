package de.ii.xtraplatform.ogc.csw.parser;

import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.ogc.api.OWS;
import de.ii.xtraplatform.ogc.api.wfs.parser.AbstractWfsCapabilitiesAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public class CSWCapabilitiesAnalyzer extends AbstractWfsCapabilitiesAnalyzer {

    String title;
    CSW.VERSION version;
    String url;
    List<String> outputFormats;
    List<String> typeNames;
    List<String> constraintLanguages;
    Map<String, String> isoQueryables;

    CSWCapabilitiesAnalyzer() {
        this.outputFormats = new ArrayList<>();
        this.typeNames = new ArrayList<>();
        this.constraintLanguages = new ArrayList<>();
        this.isoQueryables = new HashMap<>();
    }

    @Override
    public void analyzeStart() {

    }

    @Override
    public void analyzeEnd() {

    }

    @Override
    public void analyzeVersion(String version) {
        this.version = CSW.VERSION.fromString(version);
    }

    @Override
    public void analyzeTitle(String title) {
        this.title = title;
    }

    @Override
    public void analyzeOperationGetUrl(OWS.OPERATION operation, String url) {
        if (operation == OWS.OPERATION.GET_RECORDS) {
            this.url = url;
        }
    }

    @Override
    public void analyzeOperationPostUrl(OWS.OPERATION operation, String url) {
        if (operation == OWS.OPERATION.GET_RECORDS && this.url == null) {
            this.url = url;
        }
    }

    @Override
    public void analyzeOperationParameter(OWS.OPERATION operation, OWS.VOCABULARY parameterName, String value) {
        if (operation == OWS.OPERATION.GET_RECORDS && parameterName == OWS.VOCABULARY.OUTPUT_FORMAT) {
            this.outputFormats.add(value);
        }
        if (operation == OWS.OPERATION.GET_RECORDS && parameterName == OWS.VOCABULARY.TYPENAMES) {
            this.typeNames.add(value);
        }
        if (operation == OWS.OPERATION.GET_RECORDS && (parameterName == OWS.VOCABULARY.CONSTRAINTLANGUAGE || parameterName == OWS.VOCABULARY.CONSTRAINT_LANGUAGE)) {
            this.constraintLanguages.add(value.toUpperCase());
        }
    }

    @Override
    public void analyzeOperationConstraint(OWS.OPERATION operation, OWS.VOCABULARY constraintName, String value) {
        if (operation == OWS.OPERATION.GET_RECORDS && constraintName == OWS.VOCABULARY.SUPPORTED_ISO_QUERYABLES) {
            if (value.toLowerCase().contains(CSW.getWord(CSW.VOCABULARY.SERVICE_TYPE_VERSION).toLowerCase())) {
                this.isoQueryables.putIfAbsent(CSW.getWord(CSW.VOCABULARY.SERVICE_TYPE_VERSION), value);
            } else if (value.toLowerCase().contains(CSW.getWord(CSW.VOCABULARY.SERVICE_TYPE).toLowerCase())) {
                this.isoQueryables.putIfAbsent(CSW.getWord(CSW.VOCABULARY.SERVICE_TYPE), value);
            } else if (value.toLowerCase().startsWith(CSW.getWord(CSW.VOCABULARY.TYPE).toLowerCase()) || value.toLowerCase().endsWith(":" + CSW.getWord(CSW.VOCABULARY.TYPE).toLowerCase())) {
                this.isoQueryables.putIfAbsent(CSW.getWord(CSW.VOCABULARY.TYPE), value);
            }
        }
    }
}
