/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app

import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.features.domain.*

class FeatureStoreFixtures {

    static FeatureStoreInstanceContainer SIMPLE_GEOMETRY = ImmutableFeatureStoreInstanceContainer.builder()
            .name("building")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("location")
                    .path(ImmutableList.of("building", "location"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer JOINED_GEOMETRY = ImmutableFeatureStoreInstanceContainer.builder()
            .name("building")
            .sortKey("id")
            .addRelatedContainers(ImmutableFeatureStoreRelatedContainer.builder()
                    .name("geometry")
                    .sortKey("id")
                    .addInstanceConnection(ImmutableFeatureStoreRelation.builder()
                            .cardinality(FeatureStoreRelation.CARDINALITY.ONE_2_ONE)
                            .sourceContainer("building")
                            .sourceField("id")
                            .targetContainer("geometry")
                            .targetField("id")
                            .build())
                    .addAttributes(ImmutableFeatureStoreAttribute.builder()
                            .name("location")
                            .path(ImmutableList.of("geometry", "location"))
                            .build())
                    .build())
            .build();

    static FeatureStoreInstanceContainer SIMPLE_INSTANT = ImmutableFeatureStoreInstanceContainer.builder()
            .name("building")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("built")
                    .path(ImmutableList.of("building", "built"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer SIMPLE_INTERVAL = ImmutableFeatureStoreInstanceContainer.builder()
            .name("building")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("updated")
                    .path(ImmutableList.of("building", "updated"))
                    .build())
            .build()

}
