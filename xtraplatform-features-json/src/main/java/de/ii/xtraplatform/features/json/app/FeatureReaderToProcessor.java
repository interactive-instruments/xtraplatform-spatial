/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.json.app;

import de.ii.xtraplatform.features.domain.CollectionMetadata;
import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureProcessor;
import de.ii.xtraplatform.features.domain.FeatureReader;
import de.ii.xtraplatform.features.domain.ModifiableCollectionMetadata;
import de.ii.xtraplatform.features.domain.Property;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

//TODO: FeatureMapper
public class FeatureReaderToProcessor<T extends PropertyBase<T, V>, U extends FeatureBase<T, V>, V extends SchemaBase<V>> implements FeatureReader<V, V> {

    private final FeatureProcessor<T, U, V> featureProcessor;

    private CollectionMetadata collectionMetadata;
    private U currentFeature;
    private T currentProperty;

    public FeatureReaderToProcessor(FeatureProcessor<T, U, V> featureProcessor) {
        this.featureProcessor = featureProcessor;
    }


    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched, V context) throws Exception {
        if (numberReturned.isPresent() || numberMatched.isPresent()) {
            this.collectionMetadata = ModifiableCollectionMetadata.create()
                                                                  .numberReturned(numberReturned)
                                                                  .numberMatched(numberMatched);
        }
    }

    @Override
    public void onEnd() throws Exception {
        boolean br = true;
    }

    @Override
    public void onFeatureStart(List<String> path, V context) {
        this.currentFeature = featureProcessor.createFeature();
        currentFeature.schema(context);

        if (Objects.nonNull(collectionMetadata)) {
            currentFeature.collectionMetadata(collectionMetadata);
        }

        this.currentProperty = null;
    }

    @Override
    public void onFeatureEnd(List<String> path) throws Exception {
        //TODO: cache and sort tokens
        featureProcessor.process(currentFeature);
    }

    @Override
    public void onObjectStart(List<String> path, V context) {
        this.currentProperty = createProperty(PropertyBase.Type.OBJECT, path, context);
    }

    @Override
    public void onObjectEnd(List<String> path,
        Map<String, String> context) throws Exception {
        this.currentProperty = getCurrentParent();
    }

    @Override
    public void onArrayStart(List<String> path, V context) {
        this.currentProperty = createProperty(PropertyBase.Type.ARRAY, path, context);
    }

    @Override
    public void onArrayEnd(List<String> path,
        Map<String, String> context) throws Exception {
        this.currentProperty = getCurrentParent();
    }

    @Override
    public void onValue(List<String> path, String value, V context) {
        createProperty(PropertyBase.Type.VALUE, path, context, value);
    }

    private T createProperty(Property.Type type, List<String> path, V schema) {
        return createProperty(type, path, schema, null);
    }

    private T createProperty(Property.Type type, List<String> path, V schema, String value) {

        return currentFeature.getProperties()
                             .stream()
                             .filter(t -> t.getType() == type && t.getSchema().isPresent() && Objects.equals(t.getSchema().get(), schema) && !t.getSchema().get().isSpatial())
                             .findFirst()
                             .orElseGet(() -> {
                                       T property = featureProcessor.createProperty();
                                       //TODO: multiple properties for same path
                                       property.type(type)
                                                .schema(schema);
                                       if (type == PropertyBase.Type.VALUE && Objects.nonNull(value)) {
                                           property.value(value);
                                       }

                                       if (Objects.nonNull(currentProperty)) {
                                           property.parent(currentProperty);
                                           currentProperty.addNestedProperties(property);
                                       } else {
                                           currentFeature.addProperties(property);
                                       }

                                       return property;
                                   });
    }

    private T getCurrentParent() {
        return Objects.nonNull(currentProperty) && currentProperty.getParent()
                                                                  .isPresent()
                ? currentProperty.getParent()
                                 .get()
                : null;
    }
}
