/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import akka.Done;
import akka.NotUsed;
import akka.japi.function.Function;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.transformer.api.GmlStreamParser;
import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.FeatureStream2;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureType;
import de.ii.xtraplatform.features.domain.ImmutableResult;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class FeatureNormalizerWfs implements FeatureNormalizer<ByteString> {

    private final Map<String, FeatureStoreTypeInfo> typeInfos;
    private final Map<String, FeatureType> types;
    private final Map<String, FeatureSchema> schemas;
    private final Map<String, String> namespaces;
    private final XMLNamespaceNormalizer namespaceNormalizer;

    public FeatureNormalizerWfs(Map<String, FeatureStoreTypeInfo> typeInfos, Map<String, FeatureType> types,
                                Map<String, FeatureSchema> schemas, Map<String, String> namespaces) {
        this.typeInfos = typeInfos;
        this.types = types;
        this.schemas = schemas;
        this.namespaces = namespaces;
        this.namespaceNormalizer = new XMLNamespaceNormalizer(namespaces);
    }

    private String resolveNamespaces(String path) {

        String resolvedPath = path;

        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            String prefix = entry.getKey();
            String uri = entry.getValue();
            resolvedPath = resolvedPath.replaceAll(prefix, uri);
        }

        return resolvedPath;
    }

    @Override
    public Sink<ByteString, CompletionStage<FeatureStream2.Result>> normalizeAndTransform(
            FeatureTransformer2 featureTransformer, FeatureQuery featureQuery) {

        FeatureType featureType = types.get(featureQuery.getType());
        FeatureStoreTypeInfo typeInfo = typeInfos.get(featureQuery.getType());
        FeatureSchema featureSchema = schemas.get(featureQuery.getType());

        String name = featureSchema.getSourcePath().map(sourcePath -> sourcePath.substring(1)).orElse(null);

        QName qualifiedName = new QName(namespaceNormalizer.getNamespaceURI(namespaceNormalizer.extractURI(name)), namespaceNormalizer.getLocalName(name));
        String featureTypePath = qualifiedName.getNamespaceURI() + ":" + qualifiedName.getLocalPart();

        //TODO
        ImmutableFeatureType featureType1 = new ImmutableFeatureType.Builder().from(featureType)
                                                                              .additionalInfo(namespaces)
                                                                              .putAdditionalInfo("featureTypePath", featureTypePath)
                                                                              .build();


        Sink<ByteString, CompletionStage<Done>> transformer = GmlStreamParser.transform(qualifiedName, featureType1, featureTransformer, featureQuery.getFields(), ImmutableMap.of());

        return transformer.mapMaterializedValue((Function<CompletionStage<Done>, CompletionStage<FeatureStream2.Result>>) (completionStage) -> completionStage.handle((done, throwable) -> {
            return ImmutableResult.builder()
                                  .isEmpty(false/*TODO!readContext.getReadState()
                                                       .isAtLeastOneFeatureWritten()*/)
                                  .error(Optional.ofNullable(throwable))
                                  .build();
        }));
    }

    @Override
    public <V extends PropertyBase<V, X>, W extends FeatureBase<V, X>, X extends SchemaBase<X>> Source<W, CompletionStage<FeatureStream2.Result>> normalize(Source<ByteString, NotUsed> sourceStream, FeatureQuery featureQuery, Supplier<W> featureCreator, Supplier<V> propertyCreator) {
        return null;
    }
}
