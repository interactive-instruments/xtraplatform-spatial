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
package de.ii.xtraplatform.ogc.api.gml.parser;

import de.ii.xtraplatform.ogc.api.exceptions.SchemaParseException;
import de.ii.xtraplatform.ogc.api.i18n.FrameworkMessages;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
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
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.GML;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.forgerock.i18n.slf4j.LocalizedLogger;
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

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(GMLSchemaParser.class);
    private final List<GMLSchemaAnalyzer> analyzers;
    private final XMLPathTracker currentPath;
    private final List<XSElementDecl> abstractObjectDecl;
    private XSType gcoObjectType;
    private Set<String> complexTypes;
    private final URI baseURI;
    private EntityResolver entityResolver;

    public GMLSchemaParser(List<GMLSchemaAnalyzer> analyzers, URI baseURI) {
        this(analyzers, baseURI, new OGCEntityResolver());
    }

    public GMLSchemaParser(List<GMLSchemaAnalyzer> analyzers, URI baseURI, EntityResolver entityResolver) {
        this.analyzers = analyzers;
        this.currentPath = new XMLPathTracker();
        this.abstractObjectDecl = new ArrayList<XSElementDecl>();
        this.baseURI = baseURI;
        this.entityResolver = entityResolver;
    }

    public void parse(HttpEntity entity, Map<String, List<String>> elements) {
        try {
            InputSource is = new InputSource(entity.getContent());
            parse(is, elements);
        } catch (IOException ex) {
            LOGGER.error(FrameworkMessages.ERROR_PARSING_APPLICATION_SCHEMA, ex);
            throw new SchemaParseException(FrameworkMessages.ERROR_PARSING_APPLICATION_SCHEMA, ex.getMessage());
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
    }

    public void parse(InputSource is, Map<String, List<String>> elements) {
        parse(is, elements, true);
    }

    public void parse(InputSource is, Map<String, List<String>> elements, boolean lax) {
        //LOGGER.debug("Parsing GML application schema");
        XSOMParser parser = new XSOMParser();

        try {
            parser.setErrorHandler(new GMLSchemaParserErrorHandler());

            parser.setEntityResolver(entityResolver);

            is.setSystemId(baseURI.toString());

            parser.parse(is);

            XSSchemaSet schemas = parser.getResult();

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

            for (Map.Entry<String, List<String>> ns : elements.entrySet()) {
                String nsuri = ns.getKey();
                String oldNsUri = null;
                //LOGGER.debug("namespace {}", nsuri);

                XSSchema schema = schemas.getSchema(nsuri);

                // workaround for broken WFSs where FeatureTypes are in different namespaces in Capabilities and Schema
                // in this case we search in the targetNamespace of the Schema
                if (schema == null && lax) {
                    LOGGER.info(FrameworkMessages.SCHEMA_FOR_NAMESPACE_NOT_FOUND_RETRYING, ns.getKey());

                    // looks as if the schema for the targetNamespace of the document is always second in the list
                    schema = schemas.getSchema(1);
                    oldNsUri = nsuri;
                    nsuri = schema.getTargetNamespace();
                }

                for (String e : ns.getValue()) {
                    XSElementDecl elem = schema.getElementDecl(e);
                    if (elem != null && elem.getType().isComplexType()) {
                        //LOGGER.debug(" - element {}, type: {}", elem.getName(), elem.getType().getName());

                        for (GMLSchemaAnalyzer analyzer : analyzers) {
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
                LOGGER.error(FrameworkMessages.ERROR_PARSING_APPLICATION_SCHEMA_FILE_NOT_FOUND, ex.getCause().getMessage());
                throw new SchemaParseException(FrameworkMessages.ERROR_PARSING_APPLICATION_SCHEMA_FILE_NOT_FOUND, ex.getCause().getMessage());
            }
            
            String msg = ex.getMessage();
            String msgex = "";
            if( msg != null && !msg.isEmpty()){
                msg = "Parser details: " + msg;
                
                msgex = msg.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            }
 
            LOGGER.error(FrameworkMessages.ERROR_PARSING_APPLICATION_SCHEMA_PARSE_EXCEPTION, msg);
            SchemaParseException spe = new SchemaParseException(FrameworkMessages.ERROR_PARSING_APPLICATION_SCHEMA_PARSE_EXCEPTION, "");

            spe.addDetail(msgex);
            
            throw spe;
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
                propertyType = propertyType.asSimpleType().getPrimitiveType();
                propertyTypeName = propertyType.getName();
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

        for (GMLSchemaAnalyzer analyzer : analyzers) {

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
