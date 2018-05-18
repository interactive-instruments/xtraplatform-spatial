package de.ii.xtraplatform.ogc.api.wfs.client;

import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLDocumentFactory;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Map;

/**
 * @author zahnen
 */
public interface WfsOperation {
    WFS.OPERATION getOperation();

    default String getOperationName(WFS.VERSION version) {
        return WFS.getWord(version, this.getOperation());
    }

    XMLDocument asXml(XMLDocumentFactory documentFactory, Versions versions) throws TransformerException, IOException, SAXException;

    Map<String,String> asKvp(XMLDocumentFactory documentFactory, Versions versions) throws TransformerException, IOException, SAXException;
}
