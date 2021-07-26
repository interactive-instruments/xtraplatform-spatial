/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface FeatureStoreRelation {

    enum CARDINALITY {
        ONE_2_ONE,
        ONE_2_N,
        M_2_N
    }

    CARDINALITY getCardinality();

    String getSourceContainer();

    String getSourceField();

    Optional<String> getSourceFilter();

    @Value.Default
    default String getSourceSortKey() {
        return getSourceField();
    }

    String getTargetContainer();

    String getTargetField();

    Optional<String> getJunction();

    Optional<String> getJunctionSource();

    Optional<String> getJunctionTarget();

    @Value.Check
    default void check() {
        Preconditions.checkState((getCardinality() == CARDINALITY.M_2_N && getJunction().isPresent()) || (!getJunction().isPresent()),
                "when a junction is set, cardinality needs to be M_2_N, when no junction is set, cardinality is not allowed to be M_2_N");
    }

    @Value.Lazy
    default boolean isOne2One() {
        return getCardinality() == CARDINALITY.ONE_2_ONE;
    }

    @Value.Lazy
    default boolean isOne2N() {
        return getCardinality() == CARDINALITY.ONE_2_N;
    }

    @Value.Lazy
    default boolean isM2N() {
        return getCardinality() == CARDINALITY.M_2_N;
    }

    Optional<CqlFilter> getFilter();

    @Value.Derived
    default List<String> asPath() {
        if (isM2N()) {
            return ImmutableList.of(
                    String.format("[%s=%s]%s", getSourceField(), getJunctionSource().get(), getJunction().get()),
                    String.format("[%s=%s]%s", getJunctionTarget().get(), getTargetField(), getTargetContainer())
            );
        }

        return ImmutableList.of(String.format("[%s=%s]%s", getSourceField(), getTargetField(), getTargetContainer()));
    }
}
