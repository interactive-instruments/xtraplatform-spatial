/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

public interface FeatureBase<T extends PropertyBase<T,U>, U extends SchemaBase<U>> {

    Optional<CollectionMetadata> getCollectionMetadata();

    Optional<U> getSchema();

    @Value.Default
    default String getName() {
        return getSchema().map(U::getName).orElse("");
    }

    //long getIndex();

    List<T> getProperties();

    //List<T> getPropertiesByRoles(FeatureProperty.Role... roles);


/*
    @Value.Derived
    @Value.Auxiliary
    String getId();

    @Value.Derived
    @Value.Auxiliary
    Optional<Object> getSpatial();

    @Value.Derived
    @Value.Auxiliary
    Optional<Object> getSingleTemporal();

    @Value.Derived
    @Value.Auxiliary
    Optional<Object> getTemporalIntervalStart();

    @Value.Derived
    @Value.Auxiliary
    Optional<Object> getTemporalIntervalEnd();
*/


    FeatureBase<T,U> collectionMetadata(CollectionMetadata collectionMetadata);

    FeatureBase<T,U> schema(Optional<U> schema);

    FeatureBase<T,U> schema(U schema);

    FeatureBase<T,U> name(String name);

    //Feature<T> setIndex(long index);

    FeatureBase<T,U> addProperties(T property);

}
