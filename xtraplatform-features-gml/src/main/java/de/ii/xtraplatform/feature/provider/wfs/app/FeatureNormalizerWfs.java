/**
 * Copyright 2022 interactive instruments GmbH
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
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStream2.ResultOld;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableResultOld;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import javax.xml.namespace.QName;

public class FeatureNormalizerWfs implements FeatureNormalizer<byte[]> {

    private final Map<String, FeatureSchema> schemas;
    private final Map<String, String> namespaces;
    private final XMLNamespaceNormalizer namespaceNormalizer;

    public FeatureNormalizerWfs(Map<String, FeatureSchema> schemas, Map<String, String> namespaces) {
        this.schemas = schemas;
        this.namespaces = namespaces;
        this.namespaceNormalizer = new XMLNamespaceNormalizer(namespaces);
    }

    @Override
    public Sink<byte[], CompletionStage<ResultOld>> normalizeAndTransform(
            FeatureTransformer2 featureTransformer, FeatureQuery featureQuery) {

        FeatureSchema featureSchema = schemas.get(featureQuery.getType());
        String name = featureSchema.getSourcePath().map(sourcePath -> sourcePath.substring(1)).orElse(null);
        QName qualifiedName = new QName(namespaceNormalizer.getNamespaceURI(namespaceNormalizer.extractURI(name)), namespaceNormalizer.getLocalName(name));
        String featureTypePath = qualifiedName.getNamespaceURI() + ":" + qualifiedName.getLocalPart();

        //TODO
        ImmutableFeatureSchema featureSchema1 = new ImmutableFeatureSchema.Builder().from(featureSchema)
                                                                              .additionalInfo(namespaces)
                                                                              .putAdditionalInfo("featureTypePath", featureTypePath)
                                                                              .build();


        Sink<byte[], CompletionStage<Done>> transformer = GmlStreamParser.transform(qualifiedName, featureSchema1, featureTransformer, featureQuery.getFields(), featureQuery.skipGeometry(), ImmutableMap.of());

        return transformer.mapMaterializedValue((Function<CompletionStage<Done>, CompletionStage<ResultOld>>) (completionStage) -> completionStage.handle((done, throwable) -> {
            return ImmutableResultOld.builder()
                                  .isEmpty(false/*TODO!readContext.getReadState()
                                                       .isAtLeastOneFeatureWritten()*/)
                                  .error(Optional.ofNullable(throwable))
                                  .build();
        }));
    }

    public Sink<byte[], CompletionStage<ResultOld>> normalizeAndConsume(
        FeatureConsumer featureConsumer, FeatureQuery featureQuery) {
        FeatureSchema featureSchema = schemas.get(featureQuery.getType());

        String name = featureSchema.getSourcePath().map(sourcePath -> sourcePath.substring(1)).orElse(null);

        QName qualifiedName = new QName(namespaceNormalizer.getNamespaceURI(namespaceNormalizer.extractURI(name)), namespaceNormalizer.getLocalName(name));

        Sink<byte[], CompletionStage<Done>> transformer = GmlStreamParser.consume(qualifiedName, featureConsumer);

        return transformer.mapMaterializedValue((Function<CompletionStage<Done>, CompletionStage<ResultOld>>) (completionStage) -> completionStage.handle((done, throwable) -> {
            return ImmutableResultOld.builder()
                .isEmpty(false/*TODO!readContext.getReadState()
                                                       .isAtLeastOneFeatureWritten()*/)
                .error(Optional.ofNullable(throwable))
                .build();
        }));
    }

    @Override
    public <V extends PropertyBase<V, X>, W extends FeatureBase<V, X>, X extends SchemaBase<X>> Source<W, CompletionStage<ResultOld>> normalize(Source<byte[], NotUsed> sourceStream, FeatureQuery featureQuery, Supplier<W> featureCreator, Supplier<V> propertyCreator) {
        return null;
    }
}
