package de.ii.xtraplatform.features.domain;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

public interface Property<T extends Feature<? extends Property<T>>> {

    FeatureProperty getSchema();

    long getIndex();

    int getDepth();

    Optional<T> getParentFeature();

    Optional<? extends Property<T>> getParentProperty();

    List<? extends Property<T>> getNestedProperties();

    @Value.Default
    default String getName() {
        return getSchema().getName();
    }

    String getValue();


    Property<T> setSchema(FeatureProperty schema);

    Property<T> setName(String name);

    Property<T> setValue(String value);
}
