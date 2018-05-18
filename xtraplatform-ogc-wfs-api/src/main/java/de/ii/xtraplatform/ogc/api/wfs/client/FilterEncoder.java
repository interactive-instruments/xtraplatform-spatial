package de.ii.xtraplatform.ogc.api.wfs.client;

import com.vividsolutions.jts.geom.Envelope;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.ogc.api.WFS;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDParticle;
import org.geotools.filter.v2_0.bindings.BBOXTypeBinding;
import org.geotools.filter.v2_0.bindings.BinaryComparisonOpTypeBinding;
import org.geotools.filter.v2_0.bindings.LiteralBinding;
import org.geotools.filter.v2_0.bindings.PropertyIsLikeTypeBinding;
import org.geotools.geometry.jts.LiteCoordinateSequence;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml3.v3_2.GML;
import org.geotools.gml3.v3_2.bindings.EnvelopeTypeBinding;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.spatial.BBOX;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.picocontainer.MutablePicoContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zahnen
 */
public class FilterEncoder {
    private final Encoder encoder;
    private final WFS.VERSION version;

    // TODO: bugfixes for 1.0 and 1.1
    public FilterEncoder(WFS.VERSION version) {
        this.version = version;

        Configuration configuration;

        switch (version) {
            case _1_0_0:
                configuration = new org.geotools.filter.v1_0.OGCConfiguration();
                break;
            case _1_1_0:
                configuration = new org.geotools.filter.v1_1.OGCConfiguration();
                break;
            case _2_0_0:
            default:
                configuration = new org.geotools.filter.v2_0.FESConfiguration() {
                    @Override
                    protected void configureBindings(MutablePicoContainer container) {
                        super.configureBindings(container);
                        container.unregisterComponent(org.geotools.filter.v2_0.FES.BBOXType);
                        container.registerComponentImplementation(org.geotools.filter.v2_0.FES.BBOXType, MyBBOXTypeBinding.class);
                        container.unregisterComponent(GML.EnvelopeType);
                        container.registerComponentImplementation(GML.EnvelopeType, MyEnvelopeTypeBinding.class);
                        container.unregisterComponent(org.geotools.filter.v2_0.FES.BinaryComparisonOpType);
                        container.registerComponentImplementation(org.geotools.filter.v2_0.FES.BinaryComparisonOpType, MyPropertyIsEqualToBinding.class);
                        container.unregisterComponent(org.geotools.filter.v2_0.FES.PropertyIsLikeType);
                        container.registerComponentImplementation(org.geotools.filter.v2_0.FES.PropertyIsLikeType, MyPropertyIsLikeBinding.class);
                        container.unregisterComponent(org.geotools.filter.v2_0.FES.Literal);
                        container.registerComponentImplementation(org.geotools.filter.v2_0.FES.Literal, MyLiteralBinding.class);
                    }
                };
        }

        this.encoder = new Encoder(configuration);


    }

    public Element encode(Filter filter) throws TransformerException, IOException, SAXException {
        return encoder.encodeAsDOM(filter, new QName(FES.getNS(version.getFilterVersion()), FES.getWord(version.getFilterVersion(), FES.VOCABULARY.FILTER), FES.getPR(version.getFilterVersion())))
                      .getDocumentElement();
    }

    public String encodeAsString(Filter filter) throws IOException {
        return encoder.encodeAsString(filter, new QName(FES.getNS(version.getFilterVersion()), FES.getWord(version.getFilterVersion(), FES.VOCABULARY.FILTER), FES.getPR(version.getFilterVersion())));
    }

    public static class MyBBOXTypeBinding extends BBOXTypeBinding {
        public MyBBOXTypeBinding() {
            super();
        }

        @Override
        public Object getProperty(Object object, QName name) throws Exception {
            org.opengis.filter.spatial.BBOX box = (org.opengis.filter.spatial.BBOX) object;
            return box.getExpression1();
        }

        @Override
        public List getProperties(Object object, XSDElementDeclaration element) throws Exception {
            BBOX box = (BBOX) object;
            List properties = new ArrayList();
            Object env = null;

            if (box.getSRS() != null) {
                env = new CrsEnvelope(box.getMinX(), box.getMaxX(), box.getMinY(), box.getMaxY(), box.getSRS());
            } else {
                env = new CrsEnvelope(box.getMinX(), box.getMaxX(), box.getMinY(), box.getMaxY(), new EpsgCrs(4326).getAsUri());
            }

            properties.add(new Object[]{ENVELOPE_PARTICLE2, env});
            return properties;
        }

        static final XSDParticle ENVELOPE_PARTICLE2;

        static {
            ENVELOPE_PARTICLE2 = XSDFactory.eINSTANCE.createXSDParticle();
            ENVELOPE_PARTICLE2.setMinOccurs(0);
            ENVELOPE_PARTICLE2.setMaxOccurs(-1);

            try {
                ENVELOPE_PARTICLE2.setContent(GML.getInstance()
                                                 .getSchema()
                                                 .resolveElementDeclaration(GML.Envelope.getLocalPart()));
            } catch (IOException var1) {
                throw new RuntimeException(var1);
            }
        }
    }

    public static class MyEnvelopeTypeBinding extends EnvelopeTypeBinding {

        public MyEnvelopeTypeBinding(Configuration config, SrsSyntax srsSyntax) {
            super(config, srsSyntax);
        }

        @Override
        public Object getProperty(Object object, QName name) {
            Envelope envelope = (Envelope) object;
            if (envelope.isNull()) {
                return null;
            } else if (name.getLocalPart()
                           .equals("lowerCorner")) {
                return new LiteCoordinateSequence(new double[]{envelope.getMinX(), envelope.getMinY()}, 2);
            } else if (name.getLocalPart()
                           .equals("upperCorner")) {
                return new LiteCoordinateSequence(new double[]{envelope.getMaxX(), envelope.getMaxY()}, 2);
            } else {
                if (envelope instanceof CrsEnvelope) {
                    String localName = name.getLocalPart();
                    if (localName.equals("srsName")) {
                        return ((CrsEnvelope) envelope).getCrs();
                    }

                    if (localName.equals("srsDimension")) {
                        return null;
                    }
                }

                return null;
            }
        }
    }

    public static class MyLiteralBinding extends LiteralBinding {
        public static int instantCounter = 1;
        public static int periodCounter = 1;

        public MyLiteralBinding(FilterFactory factory) {
            super(factory);
        }

        @Override
        public Element encode(Object object, Document document, Element value) throws Exception {
            Object unconvertedValue = ((Literal)object).getValue();
            if (unconvertedValue != null) {
                if (unconvertedValue instanceof Instant) {
                    return createInstant(document, (Instant) unconvertedValue);
                }
                if (unconvertedValue instanceof Period) {
                    return createPeriod(document, (Period) unconvertedValue);
                }
            }

            return super.encode(object, document, value);
        }

        private Element createInstant(Document document, Instant instant) {
            Element instantElement = document.createElementNS(org.geotools.gml3.v3_2.GML.TimeInstant.getNamespaceURI(), "gml:" + org.geotools.gml3.v3_2.GML.TimeInstant.getLocalPart());
            instantElement.setAttributeNS(org.geotools.gml3.v3_2.GML.TimeInstant.getNamespaceURI(), "gml:id", "TI" + instantCounter++);
            Element position = (Element)instantElement.appendChild(document.createElementNS(org.geotools.gml3.v3_2.GML.timePosition.getNamespaceURI(), "gml:" + org.geotools.gml3.v3_2.GML.timePosition.getLocalPart()));
            position.appendChild(document.createTextNode(instant.getPosition().getDate().toInstant().toString()));

            return instantElement;
        }

        private Element createPeriod(Document document, Period period) {
            Element periodElement = document.createElementNS(GML.TimePeriod.getNamespaceURI(), "gml:" + org.geotools.gml3.v3_2.GML.TimePeriod.getLocalPart());
            periodElement.setAttributeNS(org.geotools.gml3.v3_2.GML.TimePeriod.getNamespaceURI(), "gml:id", "TP" + periodCounter++);
            Element begin = (Element)periodElement.appendChild(document.createElementNS(org.geotools.gml3.v3_2.GML.TimePeriod.getNamespaceURI(), "gml:begin"));
            begin.appendChild(createInstant(document, period.getBeginning()));
            Element end = (Element)periodElement.appendChild(document.createElementNS(org.geotools.gml3.v3_2.GML.TimePeriod.getNamespaceURI(), "gml:end"));
            end.appendChild(createInstant(document, period.getEnding()));

            return periodElement;
        }
    }

    public static class MyPropertyIsEqualToBinding extends BinaryComparisonOpTypeBinding {

        @Override
        public Object getProperty(Object object, QName name) throws Exception {
            if ("matchAction".equals(name.getLocalPart())) {
                return null;
            } else if ("matchCase".equals(name.getLocalPart())) {
                return null;
            } else {
                return super.getProperty(object, name);
            }
        }
    }

    public static class MyPropertyIsLikeBinding extends PropertyIsLikeTypeBinding {

        public MyPropertyIsLikeBinding(FilterFactory factory) {
            super(factory);
        }

        @Override
        public Object getProperty(Object object, QName name) throws Exception {
            if ("wildCard".equals(name.getLocalPart())) {
                return "*";
            } else if ("singleChar".equals(name.getLocalPart())) {
                return "#";
            } else if ("matchCase".equals(name.getLocalPart())) {
                return null;
            }

            return super.getProperty(object, name);
        }
    }

    public static class CrsEnvelope extends Envelope {
        String crs;

        public CrsEnvelope(double minX, double maxX, double minY, double maxY, String crs) {
            super(minX, maxX, minY, maxY);
            this.crs = crs;
        }

        public String getCrs() {
            return crs;
        }
    }
}
