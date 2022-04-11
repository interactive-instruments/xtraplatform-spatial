/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.app;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.gml.domain.GmlGeometryType;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder;
import de.ii.xtraplatform.features.domain.ImmutableSchemaMapping;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.features.gml.domain.XMLNamespaceNormalizer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * @author zahnen
 */
public class FeatureTokenDecoderGml extends FeatureTokenDecoder<byte[], FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>> {

    static final List<String> GEOMETRY_PARTS = new ImmutableList.Builder<String>()
        .add("exterior")
        .add("interior")
        .add("outerBoundaryIs")
        .add("innerBoundaryIs")
        .add("pointMember")
        .build();
    static final List<String> GEOMETRY_COORDINATES = new ImmutableList.Builder<String>()
        .add("posList")
        .add("pos")
        .add("coordinates")
        .build();

    private final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser;

    private final XMLNamespaceNormalizer namespaceNormalizer;
    private final FeatureSchema featureSchema;
    private final FeatureQuery featureQuery;
    private final List<QName> featureTypes;
    private final StringBuilder buffer;
    private final GmlMultiplicityTracker multiplicityTracker;
    private final boolean passThrough;

    private boolean isBuffering;
    private int depth = 0;
    private int featureDepth = 0;
    private boolean inFeature = false;
    private ModifiableContext<FeatureSchema, SchemaMapping> context;
    private FeatureSchema currentGeometrySchema;
    private SimpleFeatureGeometry currentGeometryType;

    public FeatureTokenDecoderGml(Map<String, String> namespaces,
        List<QName> featureTypes,
        FeatureSchema featureSchema, FeatureQuery query, boolean passThrough) {
        this.namespaceNormalizer = new XMLNamespaceNormalizer(namespaces);
        this.featureSchema = featureSchema;
        this.featureQuery = query;
        this.featureTypes = featureTypes;
        this.buffer = new StringBuilder();
        this.multiplicityTracker = new GmlMultiplicityTracker();
        this.currentGeometrySchema = null;
        this.currentGeometryType = SimpleFeatureGeometry.NONE;
        this.passThrough = passThrough;

        try {
            this.parser = new InputFactoryImpl().createAsyncFor(new byte[0]);
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Could not create GML decoder: " + e.getMessage());
        }
    }

    @Override
    protected void init() {
        this.context = createContext()
            .setMapping(new ImmutableSchemaMapping.Builder().targetSchema(featureSchema).build())
            .setQuery(featureQuery);
    }

    @Override
    protected void cleanup() {
        parser.getInputFeeder()
            .endOfInput();
    }

    @Override
    public void onPush(byte[] bytes) {
        feedInput(bytes);
    }

    // for unit tests
    void parse(String data) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        feedInput(dataBytes);
        cleanup();
    }

    private void feedInput(byte[] data) {
        try {
            parser.getInputFeeder()
                .feedInput(data, 0, data.length);
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        }

        boolean feedMeMore = false;
        while (!feedMeMore) {
            feedMeMore = advanceParser();
        }
    }

    //TODO: single feature or collection
    protected boolean advanceParser() {

        boolean feedMeMore = false;

        try {
            if (!parser.hasNext()) return true;

            switch (parser.next()) {
                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    feedMeMore = true;
                    break;

                case XMLStreamConstants.START_DOCUMENT:

                    break;

                case XMLStreamConstants.END_DOCUMENT:

                    //completeStage();
                    break;

                case XMLStreamConstants.START_ELEMENT:
                    if (depth == 0) {
                        OptionalLong numberMatched;
                        OptionalLong numberReturned;
                        try {
                            numberReturned = OptionalLong.of(Long.parseLong(parser.getAttributeValue(null, "numberReturned")));
                        } catch (NumberFormatException e) {
                            numberReturned = OptionalLong.empty();
                        }
                        try {
                            numberMatched = OptionalLong.of(Long.parseLong(parser.getAttributeValue(null, "numberMatched")));
                        } catch (NumberFormatException e) {
                            numberMatched = OptionalLong.empty();
                        }

                        context.metadata().numberReturned(numberReturned);
                        context.metadata().numberMatched(numberMatched);

                        context.additionalInfo().clear();
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            context.putAdditionalInfo(namespaceNormalizer.getQualifiedName(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i)), parser.getAttributeValue(i));
                        }

                        getDownstream().onStart(context);
                    } else if (matchesFeatureType(parser.getNamespaceURI(), parser.getLocalName())
                                || matchesFeatureType(parser.getLocalName())) {
                        inFeature = true;
                        featureDepth = depth;

                        context.additionalInfo().clear();
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            context.putAdditionalInfo(namespaceNormalizer.getQualifiedName(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i)), parser.getAttributeValue(i));
                        }

                        context.pathTracker().track(namespaceNormalizer.getQualifiedName(parser.getNamespaceURI(), parser.getLocalName()));

                        getDownstream().onFeatureStart(context);

                        if (context.additionalInfo().containsKey("gml:id") && !passThrough) {
                            context.pathTracker().track("gml:@id");
                            context.setValue(context.additionalInfo().get("gml:id"));
                            context.setValueType(Type.STRING);
                            getDownstream().onValue(context);
                        }
                    } else if (inFeature) {
                        context.pathTracker().track(namespaceNormalizer.getQualifiedName(parser.getNamespaceURI(), parser.getLocalName()), depth - featureDepth);
                        multiplicityTracker.track(context.pathTracker().asList());

                        context.additionalInfo().clear();
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            context.putAdditionalInfo(namespaceNormalizer.getQualifiedName(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i)), parser.getAttributeValue(i));
                        }

                        context.setIndexes(multiplicityTracker.getMultiplicitiesForPath(context.pathTracker().asList()));

                        if (context.schema().filter(FeatureSchema::isSpatial).isPresent()) {
                            if (Objects.isNull(currentGeometrySchema)) {
                                this.currentGeometrySchema = context.schema().get();
                                context.setInGeometry(true);
                            }
                            onGeometryPart(parser.getLocalName(), depth - featureDepth == 2);
                        } else if (passThrough) {
                            getDownstream().onObjectStart(context);
                        }
                    }
                    depth += 1;
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    if (isBuffering) {
                        this.isBuffering = false;
                        if (buffer.length() > 0) {
                            context.setValue(buffer.toString());
                            getDownstream().onValue(context);
                            buffer.setLength(0);
                        }
                    }

                    depth -= 1;
                    if (depth == 0) {
                        getDownstream().onEnd(context);
                    } else if (matchesFeatureType(parser.getLocalName())) {
                        inFeature = false;
                        getDownstream().onFeatureEnd(context);
                        multiplicityTracker.reset();
                    } else if (inFeature) {
                        if (context.schema().filter(FeatureSchema::isSpatial).isPresent()) {
                            if (Objects.nonNull(currentGeometrySchema) && (depth-featureDepth) > 1) {
                                onGeometryPartEnd(parser.getLocalName(), depth - featureDepth == 2);
                              } else {
                                this.currentGeometrySchema = null;
                                this.currentGeometryType = SimpleFeatureGeometry.NONE;
                                context.setInGeometry(false);
                                context.setGeometryType(Optional.empty());
                                context.setGeometryDimension(OptionalInt.empty());

                                getDownstream().onObjectEnd(context);
                            }
                        } else if (passThrough) {
                            getDownstream().onObjectEnd(context);
                        }
                    }

                    context.pathTracker().track(depth - featureDepth);

                    break;

                case XMLStreamConstants.CHARACTERS:
                    if (inFeature) {
                        if (!parser.isWhiteSpace()) {
                            this.isBuffering = true;
                            buffer.append(parser.getText());
                        }
                    }
                    break;

                // Do not support DTD, SPACE, NAMESPACE, NOTATION_DECLARATION, ENTITY_DECLARATION, PROCESSING_INSTRUCTION, COMMENT, CDATA
                // ATTRIBUTE is handled in START_ELEMENT implicitly

                default:
                    //advanceParser(in);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse GML: " + e.getMessage(), e);
        }
        return feedMeMore;
    }

    boolean matchesFeatureType(final String namespace, final String localName) {
        return featureTypes.stream()
                           .anyMatch(featureType -> featureType.getLocalPart()
                                                               .equals(localName) && Objects.nonNull(namespace) && featureType.getNamespaceURI()
                                                                                                                              .equals(namespace));
    }

    boolean matchesFeatureType(final String localName) {
        return featureTypes.stream()
                           .anyMatch(featureType -> featureType.getLocalPart()
                                                               .equals(localName));
    }

    QName getMatchingFeatureType(final String localName) {
        return featureTypes.stream()
                           .filter(featureType -> featureType.getLocalPart()
                                                             .equals(localName))
                           .findFirst()
                           .orElse(null);
    }

    private void onGeometryPart(final String localName, boolean startOfGeometry) throws Exception {
        if (Objects.isNull(currentGeometrySchema)) return;

        if (currentGeometryType == SimpleFeatureGeometry.NONE) {
            final SimpleFeatureGeometry geometryType = GmlGeometryType.fromString(localName)
                .toSimpleFeatureGeometry();
            if (geometryType.isValid()) {
                this.currentGeometryType = geometryType;
                context.setGeometryType(currentGeometryType);

                OptionalInt dimension = OptionalInt.empty();
                if (context.additionalInfo().containsKey("srsDimension")) {
                    try {
                        dimension = OptionalInt
                            .of(Integer.parseInt(context.additionalInfo().get("srsDimension")));
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                context.setGeometryDimension(dimension);
                context.setInGeometry(true);

                List<String> path = context.pathTracker().asList();

                context.pathTracker().track(path.size() - 1);

                if (!passThrough) {
                    getDownstream().onObjectStart(context);
                }

                context.pathTracker().track(path.get(path.size()-1));
            }
        }
        if (GEOMETRY_PARTS.contains(localName) || (startOfGeometry && currentGeometryType == SimpleFeatureGeometry.MULTI_POLYGON)) {
            getDownstream().onArrayStart(context);
        } else if (passThrough) {
            getDownstream().onObjectStart(context);
        }
    }

    private void onGeometryPartEnd(final String localName, boolean endOfGeometry) throws Exception {
        if (Objects.isNull(currentGeometrySchema)) return;

        if (GEOMETRY_PARTS.contains(localName) || (endOfGeometry && currentGeometryType == SimpleFeatureGeometry.MULTI_POLYGON)) {
            getDownstream().onArrayEnd(context);
        } else if (passThrough) {
            getDownstream().onObjectEnd(context);
        }
    }
}
