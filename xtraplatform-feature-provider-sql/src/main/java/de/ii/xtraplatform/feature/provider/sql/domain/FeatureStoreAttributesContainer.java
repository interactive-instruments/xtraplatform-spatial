package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;

public interface FeatureStoreAttributesContainer {

    String getName();

    List<String> getPath();

    String getSortKey();

    //TODO: implement predicates
    //Optional<FeatureStorePredicate> getPredicate();

    String getInstanceContainerName();

    //TODO: needed for inserts
    //boolean shouldAutoGenerateId();

    List<FeatureStoreAttribute> getAttributes();

    @Value.Derived
    @Value.Auxiliary
    default List<List<String>> getAttributePaths() {
        return getAttributes().stream()
                              .map(FeatureStoreAttribute::getPath)
                              .collect(ImmutableList.toImmutableList());
    }

    @Value.Derived
    @Value.Auxiliary
    default List<String> getSortKeys() {
        return ImmutableList.of(getSortKey());
    }
}
