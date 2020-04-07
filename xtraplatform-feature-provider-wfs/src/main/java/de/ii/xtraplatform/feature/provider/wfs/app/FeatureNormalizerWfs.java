package de.ii.xtraplatform.feature.provider.wfs.app;

import akka.Done;
import akka.NotUsed;
import akka.japi.function.Function;
import akka.japi.function.Function2;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.transformer.api.GmlStreamParser;
import de.ii.xtraplatform.features.domain.Feature;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureStream2;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProperty;
import de.ii.xtraplatform.features.domain.ImmutableFeatureType;
import de.ii.xtraplatform.features.domain.Property;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class FeatureNormalizerWfs implements FeatureNormalizer<ByteString> {

    private final Map<String, FeatureStoreTypeInfo> typeInfos;
    private final Map<String, FeatureType> types;
    private final Map<String, String> namespaces;
    private final XMLNamespaceNormalizer namespaceNormalizer;

    public FeatureNormalizerWfs(Map<String, FeatureStoreTypeInfo> typeInfos, Map<String, FeatureType> types,
                                Map<String, String> namespaces) {
        this.typeInfos = typeInfos;
        this.types = types;
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

        String name = typeInfo.getInstanceContainers()
                               .get(0)
                               .getPath()
                               .get(0);

        QName qualifiedName = new QName(namespaceNormalizer.getNamespaceURI(namespaceNormalizer.extractURI(name)), namespaceNormalizer.getLocalName(name));
        String featureTypePath = qualifiedName.getNamespaceURI() + ":" + qualifiedName.getLocalPart();

        //TODO
        ImmutableFeatureType featureType1 = new ImmutableFeatureType.Builder().from(featureType)
                                                                              .additionalInfo(namespaces)
                                                                              .putAdditionalInfo("featureTypePath", featureTypePath)
                                                                              .build();


        Sink<ByteString, CompletionStage<Done>> transformer = GmlStreamParser.transform(qualifiedName, featureType1, featureTransformer, featureQuery.getFields(), ImmutableMap.of());

        return transformer.mapMaterializedValue((Function<CompletionStage<Done>, CompletionStage<FeatureStream2.Result>>) (completionStage) -> completionStage.handle((done, throwable) -> {
            boolean success = true;
            if (Objects.nonNull(throwable)) {
                //handleException(throwable, readContext);
                success = false;
            }

            //handleCompletion(readContext);
            boolean finalSuccess = success;
            return () -> finalSuccess;
        }));
    }

    @Override
    public <U extends Property<?>, V extends Feature<U>> Source<V, CompletionStage<FeatureStream2.Result>> normalize(
            Source<ByteString, NotUsed> sourceStream, FeatureQuery featureQuery, Supplier<V> featureCreator,
            Supplier<U> propertyCreator) {
        return null;
    }
}
