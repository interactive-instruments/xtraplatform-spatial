/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app

import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.features.domain.*

class FeatureStoreFixtures {

    static FeatureStoreInstanceContainer SCALAR_OPERATIONS = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("floors")
                    .path(ImmutableList.of("container", "floors"))
                    .build())
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("owner")
                    .path(ImmutableList.of("container", "owner"))
                    .build())
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("swimming_pool")
                    .path(ImmutableList.of("container", "swimming_pool"))
                    .build())
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("material")
                    .path(ImmutableList.of("container", "material"))
                    .build())
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("geometry")
                    .path(ImmutableList.of("container", "geometry"))
                    .build())
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("height")
                    .path(ImmutableList.of("container", "height"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer FLOORS = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("floors")
                    .path(ImmutableList.of("container", "floors"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer TAXES = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("taxes")
                    .path(ImmutableList.of("container", "taxes"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer OWNER = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("owner")
                    .path(ImmutableList.of("container", "owner"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer SWIMMING_POOL = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("swimming_pool")
                    .path(ImmutableList.of("container", "swimming_pool"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer MATERIAL = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("material")
                    .path(ImmutableList.of("container", "material"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer BUILT = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("built")
                    .path(ImmutableList.of("container", "built"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer UPDATED = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("updated")
                    .path(ImmutableList.of("container", "updated"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer LOCATION = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("location")
                    .path(ImmutableList.of("container", "location"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer ROAD_CLASS = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("road_class")
                    .path(ImmutableList.of("container", "road_class"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer EVENT_DATE = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("event_date")
                    .path(ImmutableList.of("container", "event_date"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer NAMES = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("names")
                    .path(ImmutableList.of("container", "names"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer NAME = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("name")
                    .path(ImmutableList.of("container", "name"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer MEASURE = ImmutableFeatureStoreInstanceContainer.builder()
            .name("observationsubject")
            .sortKey("id")
            .addRelatedContainers(ImmutableFeatureStoreRelatedContainer.builder()
                    .name("filterValues")
                    .sortKey("id")
                    .addInstanceConnection(ImmutableFeatureStoreRelation.builder()
                            .cardinality(FeatureStoreRelation.CARDINALITY.ONE_2_N)
                            .sourceContainer("observationsubject")
                            .sourceField("id")
                            .targetContainer("filterValues")
                            .targetField("observationsubjectid")
                            .build())
                    .addAttributes(ImmutableFeatureStoreAttribute.builder()
                            .name("measure")
                            .queryable("filterValues.measure")
                            .build())
                    .addAttributes(ImmutableFeatureStoreAttribute.builder()
                            .name("property")
                            .queryable("filterValues.property")
                            .build())
                    .addAttributes(ImmutableFeatureStoreAttribute.builder()
                            .name("updated")
                            .queryable("filterValues.updated")
                            .build())
                    .addAttributes(ImmutableFeatureStoreAttribute.builder()
                            .name("location")
                            .queryable("filterValues.location")
                            .build())
                    .build())
            .build()

    static FeatureStoreInstanceContainer MEASURE_CLASSIFICATION = ImmutableFeatureStoreInstanceContainer.builder()
            .name("observationsubject")
            .sortKey("id")
            .addRelatedContainers(ImmutableFeatureStoreRelatedContainer.builder()
                    .name("filterValues")
                    .sortKey("id")
                    .addInstanceConnection(ImmutableFeatureStoreRelation.builder()
                            .cardinality(FeatureStoreRelation.CARDINALITY.ONE_2_N)
                            .sourceContainer("observationsubject")
                            .sourceField("id")
                            .targetContainer("filterValues")
                            .targetField("observationsubjectid")
                            .build())
                    .addAttributes(ImmutableFeatureStoreAttribute.builder()
                            .name("measure")
                            .queryable("filterValues.measure")
                            .build())
                    .addAttributes(ImmutableFeatureStoreAttribute.builder()
                            .name("property")
                            .queryable("filterValues.property")
                            .build())
                    .build())
            .addRelatedContainers(ImmutableFeatureStoreRelatedContainer.builder()
                    .name("classification")
                    .sortKey("id")
                    .addInstanceConnection(ImmutableFeatureStoreRelation.builder()
                            .cardinality(FeatureStoreRelation.CARDINALITY.ONE_2_N)
                            .sourceContainer("observationsubject")
                            .sourceField("id")
                            .targetContainer("filterValues")
                            .targetField("observationsubjectid")
                            .build())
                    .addInstanceConnection(ImmutableFeatureStoreRelation.builder()
                            .cardinality(FeatureStoreRelation.CARDINALITY.ONE_2_N)
                            .sourceContainer("filterValues")
                            .sourceField("id")
                            .targetContainer("classification")
                            .targetField("filtervalueid")
                            .build())
                    .addAttributes(ImmutableFeatureStoreAttribute.builder()
                            .name("classificationcodeid")
                            .queryable("filterValues.classification")
                            .build())
                    .build())
            .build()

    static FeatureStoreInstanceContainer SCENE_ID = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("landsat:scene_id")
                    .path(ImmutableList.of("container", "landsat:scene_id"))
                    .build())
            .build()

    static FeatureStoreInstanceContainer GEOMETRY = ImmutableFeatureStoreInstanceContainer.builder()
            .name("container")
            .sortKey("id")
            .addAttributes(ImmutableFeatureStoreAttribute.builder()
                    .name("geometry")
                    .path(ImmutableList.of("container", "geometry"))
                    .build())
            .build()

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
