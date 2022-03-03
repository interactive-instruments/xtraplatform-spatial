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
package de.ii.xtraplatform.features.gml.infra;

import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.parser.XSOMParser;
import de.ii.xtraplatform.features.domain.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.features.gml.infra.req.GML;
import de.ii.xtraplatform.features.gml.infra.xml.XMLPathTracker;
import de.ii.xtraplatform.services.domain.TaskProgress;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author zahnen
 */
public class GMLSchemaParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(GMLSchemaParser.class);
    private final List<FeatureProviderSchemaConsumer> analyzers;
    private final XMLPathTracker currentPath;
    private final List<XSElementDecl> abstractObjectDecl;
    private XSType gcoObjectType;
    private Set<String> complexTypes;
    private final URI baseURI;
    private EntityResolver entityResolver;

    public GMLSchemaParser(List<FeatureProviderSchemaConsumer> analyzers, URI baseURI) {
        this(analyzers, baseURI, new OGCEntityResolver());
    }

    public GMLSchemaParser(List<FeatureProviderSchemaConsumer> analyzers, URI baseURI, EntityResolver entityResolver) {
        this.analyzers = analyzers;
        this.currentPath = new XMLPathTracker();
        this.abstractObjectDecl = new ArrayList<XSElementDecl>();
        this.baseURI = baseURI;
        this.entityResolver = entityResolver;
    }

    public void parse(InputStream inputStream, Map<String, List<String>> elements, TaskProgress taskProgress) {
        try {
            taskProgress.setCompleteness(0.1);
            taskProgress.setStatusMessage("retrieving GML application schema");

            InputSource is = new InputSource(inputStream);
            parse(is, elements, taskProgress);

            for (FeatureProviderSchemaConsumer analyzer : analyzers) {
                analyzer.analyzeSuccess();
            }

            taskProgress.setCompleteness(1);
            taskProgress.setStatusMessage("success");

        } catch (Exception ex) {
            LOGGER.error("Error parsing application schema. {}", ex);

            for (FeatureProviderSchemaConsumer analyzer : analyzers) {
                analyzer.analyzeFailure(ex);
            }

            taskProgress.setCompleteness(1);
            taskProgress.setStatusMessage("failure");
        }
    }

    private void parse(InputSource is, Map<String, List<String>> elements, TaskProgress taskProgress) {
        parse(is, elements, true, taskProgress);
    }

    private void parse(InputSource is, Map<String, List<String>> elements, boolean lax, TaskProgress taskProgress) {
        //LOGGER.debug("Parsing GML application schema");
        XSOMParser parser = new XSOMParser();

        try {
            parser.setErrorHandler(new GMLSchemaParserErrorHandler());

            parser.setEntityResolver(entityResolver);

            is.setSystemId(baseURI.toString());

            parser.parse(is);

            XSSchemaSet schemas = parser.getResult();

            schemas.getSchemas().forEach(xsSchema -> analyzers.forEach(analyzer -> analyzer.analyzeNamespace(xsSchema.getTargetNamespace())));

            for (GML.VERSION version : GML.VERSION.values()) {
                XSSchema schema0 = schemas.getSchema(GML.getWord(version, GML.NAMESPACE.URI));
                if (schema0 != null) {
                    XSElementDecl a = schema0.getElementDecl(GML.getWord(version, GML.VOCABULARY.ABSTRACT_OBJECT));
                    if (a != null) {
                        abstractObjectDecl.add(a);
                    }
                }
            }

            XSSchema schema1 = schemas.getSchema("http://www.isotc211.org/2005/gco");
            if (schema1 != null) {
                gcoObjectType = schema1.getElementDecl("AbstractObject").getType();
            }

            int total = elements.values().stream().mapToInt(List::size).sum();
            double count = 1.0;
            double factor = 0.8 / total;
            for (Map.Entry<String, List<String>> ns : elements.entrySet()) {


                String nsuri = ns.getKey();
                String oldNsUri = null;
                //LOGGER.debug("namespace {}", nsuri);

                XSSchema schema = schemas.getSchema(nsuri);

                // workaround for broken WFSs where FeatureTypes are in different namespaces in Capabilities and Schema
                // in this case we search in the targetNamespace of the Schema
                if (schema == null && lax) {
                    LOGGER.info("Schema for Namespace '{}' not found, searching in targetNamespace schema instead. ", ns.getKey());

                    // looks as if the schema for the targetNamespace of the document is always second in the list
                    schema = schemas.getSchema(1);
                    oldNsUri = nsuri;
                    nsuri = schema.getTargetNamespace();
                }

                for (String e : ns.getValue()) {
                    taskProgress.setCompleteness(0.1 + (count * factor));
                    taskProgress.setStatusMessage("analyzing feature type " + e);
                    count += 1.0;

                    XSElementDecl elem = schema.getElementDecl(e);
                    if (elem != null && elem.getType().isComplexType()) {
                        //LOGGER.debug(" - element {}, type: {}", elem.getName(), elem.getType().getName());

                        for (FeatureProviderSchemaConsumer analyzer : analyzers) {
                            if (oldNsUri != null) {
                                analyzer.analyzeNamespaceRewrite(oldNsUri, nsuri, elem.getName());
                            }
                            analyzer.analyzeFeatureType(nsuri, elem.getName());
                            for (XSAttributeUse att : elem.getType().asComplexType().getAttributeUses()) {
                                //LOGGER.debug("   - attribute {}, required: {}, type: {}, ns: {}", att.getDecl().getName(), att.isRequired(), att.getDecl().getType().getName(), att.getDecl().getTargetNamespace());

                                analyzer.analyzeAttribute(att.getDecl().getTargetNamespace(),
                                        att.getDecl().getName(), att.getDecl().getType().getName(), att.isRequired());
                            }
                        }
                        XSParticle particle = elem.getType().asComplexType().getContentType().asParticle();
                        if (particle != null) {
                            XSTerm term = particle.getTerm();
                            if (term.isModelGroup()) {
                                complexTypes = new HashSet<String>();
                                parseGroup(term.asModelGroup(), 1, false);
                            }
                        }
                    }
                }
            }
        } catch (SAXException ex) {

            // File included in schema not found
            if (ex.getCause() != null && ex.getCause().getClass().getName().contains("FileNotFoundException")) {
                throw new IllegalStateException(String.format("The GML application schema provided by the WFS imports schema '%s', but that schema cannot be accessed. A valid GML application schema is required to determine the layers of the proxy service and its characteristics.. Please contact the WFS provider to correct the schema error.", ex.getCause().getMessage()));
            }
            
            String msg = ex.toString();
            String msgex = "";
            if( msg != null && !msg.isEmpty()){
                msg = "Parser details: " + msg;
                
                msgex = msg.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            }
 
            throw new IllegalStateException("The GML application schema provided by the WFS is invalid. A valid GML application schema is required to determine the layers of the proxy service and its characteristics. Please contact the WFS provider to correct the schema error. \n" + msgex);
        }
    }

    private void parseGroup(XSModelGroup xsModelGroup, int depth, boolean isParentMultible) {
        //LOGGER.debug(new String(new char[depth]).replace("\0", "  ") + " - {}", xsModelGroup.getCompositor());

        // reset already visited complex types for every first level property of the featuretype,
        // meaning for every new path that should not contain recursion
        if (depth == 2) {
            complexTypes = new HashSet<String>();
        }

        XSParticle[] particles = xsModelGroup.getChildren();

        if (particles.length > 0) {
            for (XSParticle p : particles) {
                XSTerm pterm = p.getTerm();

                if (pterm.isElementDecl()) {
                    parseElementDecl(pterm.asElementDecl(), p.getMinOccurs().longValue(), p.getMaxOccurs().longValue(), depth, isParentMultible);
                } else if (pterm.isModelGroup()) {
                    parseGroup(pterm.asModelGroup(), depth, isParentMultible);
                } else if (pterm.isModelGroupDecl()) {
                    parseGroup(pterm.asModelGroupDecl().getModelGroup(), depth, isParentMultible);
                }
            }
        }
    }

    private void parseElementDecl(XSElementDecl decl, long minOccurs, long maxOccurs, int depth, boolean isParentMultible) {

        String propertyName = null;
        XSType propertyType = null;
        boolean isObject = false;

        propertyName = decl.getName();

        String typeName = decl.getType().getBaseType().getName();
        String typeName0 = decl.getType().getName();

        propertyType = decl.getType();

        if (decl.getType().getName() != null) {
            XSElementDecl decl0 = decl.getSubstAffiliation();

            while (decl0 != null) {
                for (XSElementDecl e : abstractObjectDecl) {
                    if (decl0.equals(e)) {
                        isObject = true;
                        break;
                    }
                }
                if (isObject) {
                    break;
                }

                decl0 = decl0.getSubstAffiliation();
            }

            if (gcoObjectType != null) {
                if (propertyType.isDerivedFrom(gcoObjectType)) {
                    isObject = true;
                }
            }

        } else if (!decl.getType().getBaseType().getName().equals("anyType")) {
            propertyType = decl.getType().getBaseType();
        }

        String propertyTypeName = propertyType.getName();

        if (propertyType.isSimpleType()) {
            if (propertyType.asSimpleType().getPrimitiveType() != null) {
                propertyTypeName = propertyType.asSimpleType().getName().matches("int|long")
                        ? "int"
                        : propertyType.asSimpleType().getPrimitiveType().getName();
            } else if (propertyType.asSimpleType().asUnion() != null) {
                propertyTypeName = "string";
            }
        }

        boolean isComplex = propertyType.isComplexType();
        if (propertyType.isComplexType() && propertyType.asComplexType().getContentType().asSimpleType() != null) {
            if (propertyType.asComplexType().getContentType().asSimpleType().getPrimitiveType() != null) {
                propertyType = propertyType.asComplexType().getContentType().asSimpleType().getPrimitiveType();
                propertyTypeName = propertyType.getName();
                isComplex = propertyType.isComplexType();
            } else if (propertyType.asComplexType().getContentType().asSimpleType().asUnion() != null) {
                propertyTypeName = "string";
                isComplex = false;
            }
        }

        if (propertyType.isComplexType() && (maxOccurs > 1 || maxOccurs == -1)) {
            isParentMultible = true;
        }

        /*
         LOGGER.debug(new String(new char[depth]).replace("\0", "  ") + " - property, name: {}, type: {}, min: {}, max: {}, ns: {}",
         propertyName, propertyType.getName(), p.getMinOccurs(), p.getMaxOccurs(),
         decl.getTargetNamespace());
         * */
        propertyTypeName = propertyTypeName == null ? "" : propertyTypeName;

        for (FeatureProviderSchemaConsumer analyzer : analyzers) {

            analyzer.analyzeProperty(decl.getTargetNamespace(),
                    propertyName, propertyTypeName,
                    minOccurs, maxOccurs, depth,
                    isParentMultible, isComplex, isObject);
        }

        if (propertyType.isComplexType()) {
            parseComplexType(propertyType.asComplexType(), depth + 1, isParentMultible);
        }
    }

    private void parseComplexType(XSComplexType type, int depth, boolean isParenMultible) {
        if (type != null) {
            /*for (XSAttributeUse att : elem.getAttributeUses()) {
             LOGGER.debug("   - attribute {}, required: {}, type: {}, ns: {}", att.getDecl().getName(), att.isRequired(), att.getDecl().getType().getName(), att.getDecl().getTargetNamespace());
             analyzer.analyzeAttribute(att.getDecl().getTargetNamespace(), att.getDecl().getName(), att.getDecl().getType().getName(), att.isRequired());
             }*/

            // stop analyzing if the complex type already occurred in this path to avoid recursion
            if (complexTypes.add(type.getTargetNamespace() + ":" + type.getName())) {
                XSParticle particle = type.getContentType().asParticle();
                if (particle != null) {
                    XSTerm term = particle.getTerm();
                    if (term.isModelGroup()) {
                        parseGroup(term.asModelGroup(), depth, isParenMultible);
                    }
                }
            }
        }
    }

    private class GMLSchemaParserErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }
}
