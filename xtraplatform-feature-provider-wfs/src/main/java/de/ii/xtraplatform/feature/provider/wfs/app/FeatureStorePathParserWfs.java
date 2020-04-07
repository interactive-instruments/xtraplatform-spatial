/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreAttribute;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreInstanceContainer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeatureStorePathParserWfs implements FeatureStorePathParser {

    public FeatureStorePathParserWfs(Map<String, String> namespaces) {

    }

    @Override
    public List<FeatureStoreInstanceContainer> parse(FeatureType featureType) {

        LinkedHashMap<String, ImmutableFeatureStoreInstanceContainer.Builder> instanceContainerBuilders = new LinkedHashMap<>();

        String instanceContainerName = featureType.getName();

        List<String> path = featureType.getProperties()
                                       .values()
                                       .stream()
                                       .findFirst()
                                       .map(featureProperty -> featureProperty.getPath().substring(1, featureProperty.getPath().indexOf("/", 1)))
                                       .map(ImmutableList::of)
                                       .orElse(ImmutableList.of());

        instanceContainerBuilders.put(instanceContainerName, ImmutableFeatureStoreInstanceContainer.builder());

        /*ImmutableFeatureStoreAttribute attribute = ImmutableFeatureStoreAttribute.builder()
                                                                             .name("id")
                                                                             .path(ImmutableList.of("http://repository.gdi-de.org/schemas/adv/produkt/alkis-vereinfacht/2.0:Flurstueck"))
                                                                             .addPath("id")
                                                                             .queryable("id")
                                                                             .isId(true)
                                                                             .isSpatial(false)
                                                                             .build();
*/
        instanceContainerBuilders.get(instanceContainerName)
                                 .name(instanceContainerName)
                                 .path(path)
                                 .sortKey("none")
                                 //.attributes(ImmutableList.of(attribute))
                                 .attributesPosition(0);

        return instanceContainerBuilders.values()
                                        .stream()
                                        .map(ImmutableFeatureStoreInstanceContainer.Builder::build)
                                        .collect(Collectors.toList());
    }
}
