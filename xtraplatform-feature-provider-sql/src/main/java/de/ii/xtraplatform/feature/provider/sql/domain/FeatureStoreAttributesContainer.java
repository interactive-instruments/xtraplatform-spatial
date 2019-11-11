package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;

public interface FeatureStoreAttributesContainer {

    String getName();

    List<String> getPath();

    String getSortKey();

    //TODO
    //Optional<FeatureStorePredicate> getPredicate();

    String getInstanceContainerName();

    //TODO
    //boolean shouldAutoGenerateId();

    List<String> getAttributes();

    @Value.Derived
    @Value.Auxiliary
    default List<List<String>> getAttributePaths() {
        return getAttributes().stream()
                              .map(attribute -> new ImmutableList.Builder<String>().addAll(getPath())
                                                                                   .add(attribute)
                                                                                   .build())
                              .collect(ImmutableList.toImmutableList());
    }

    @Value.Derived
    @Value.Auxiliary
    default List<String> getSortKeys() {
        return ImmutableList.of(getSortKey());
    }
}
