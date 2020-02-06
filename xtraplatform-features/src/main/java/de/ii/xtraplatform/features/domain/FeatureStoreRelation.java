package de.ii.xtraplatform.features.domain;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

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
}
