/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.SchemaBase;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
public interface SchemaSql extends SchemaBase<SchemaSql> {

    @Override
    @Value.Derived
    default List<String> getPath() {
        return getRelation().map(FeatureStoreRelation::asPath)
                            .orElse(ImmutableList.of(getName()));
    }

    Optional<FeatureStoreRelation> getRelation();

    //TODO
    Optional<Object> getTarget();

    Optional<String> getPrimaryKey();
}
