package de.ii.xtraplatform.feature.provider.wfs.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreAttribute;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreInstanceContainer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FeatureStorePathParserWfs implements FeatureStorePathParser {

    public FeatureStorePathParserWfs(Map<String, String> namespaces) {

    }

    @Override
    public List<FeatureStoreInstanceContainer> parse(FeatureType featureType) {

        LinkedHashMap<String, ImmutableFeatureStoreInstanceContainer.Builder> instanceContainerBuilders = new LinkedHashMap<>();

        String instanceContainerName = featureType.getName();

        instanceContainerBuilders.put(instanceContainerName, ImmutableFeatureStoreInstanceContainer.builder());

        ImmutableFeatureStoreAttribute attribute = ImmutableFeatureStoreAttribute.builder()
                                                                             .name("id")
                                                                             .path(ImmutableList.of("http://repository.gdi-de.org/schemas/adv/produkt/alkis-vereinfacht/2.0:Flurstueck"))
                                                                             .addPath("id")
                                                                             .queryable("id")
                                                                             .isId(true)
                                                                             .isSpatial(false)
                                                                             .build();

        instanceContainerBuilders.get(instanceContainerName)
                                 .name(instanceContainerName)
                                 .path(ImmutableList.of("http://repository.gdi-de.org/schemas/adv/produkt/alkis-vereinfacht/2.0:Flurstueck"))
                                 .sortKey("none")
                                 .attributes(ImmutableList.of(attribute))
                                 .attributesPosition(0);

        return instanceContainerBuilders.values()
                                        .stream()
                                        .map(ImmutableFeatureStoreInstanceContainer.Builder::build)
                                        .collect(Collectors.toList());
    }
}
