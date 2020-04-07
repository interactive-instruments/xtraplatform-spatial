/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface FeatureStoreInstanceContainer extends FeatureStoreAttributesContainer {

    Optional<CqlFilter> getFilter();

    //TODO: find a better way to handle this
    @Value.Default
    default int getAttributesPosition() {
        return 0;
    }

    //TODO:
    @Value.Default
    default String getIdField() {
        return getAttributes().stream()
                              .filter(FeatureStoreAttribute::isId)
                              .map(FeatureStoreAttribute::getName)
                              .findFirst()
                              .orElse(getSortKey());
        //return getSortKey();
    }

    List<FeatureStoreRelatedContainer> getRelatedContainers();

    @Value.Derived
    @Value.Auxiliary
    default List<FeatureStoreAttributesContainer> getAllAttributesContainers() {
        return new ImmutableList.Builder<FeatureStoreAttributesContainer>()
                .addAll(getRelatedContainers().subList(0, getAttributesPosition()))
                .add(this)
                .addAll(getRelatedContainers().subList(getAttributesPosition(), getRelatedContainers().size()))
                .build();
    }

    @Override
    @Value.Derived
    default String getInstanceContainerName() {
        return getName();
    }

    @Value.Derived
    @Value.Auxiliary
    default List<String> getMultiContainerNames() {
        return getRelatedContainers()
                .stream()
                .flatMap(relatedContainer -> relatedContainer.getInstanceConnection()
                                                             .stream())
                .filter(relation -> relation.isOne2N() || relation.isM2N())
                .map(featureStoreRelation -> String.format("[%s=%s]%s", featureStoreRelation.getJunctionTarget().orElse(featureStoreRelation.getSourceField()), featureStoreRelation.getTargetField(), featureStoreRelation.getTargetContainer()))
                .distinct()
                .collect(ImmutableList.toImmutableList());
    }

    @Value.Derived
    @Value.Auxiliary
    default Optional<FeatureStoreAttributesContainer> getSpatialAttributesContainer() {
        return getAllAttributesContainers()
                .stream()
                .filter(FeatureStoreAttributesContainer::isSpatial)
                .findFirst();
    }

}
