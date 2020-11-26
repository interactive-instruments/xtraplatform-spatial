package de.ii.xtraplatform.features.domain;

import java.util.List;
import java.util.stream.Collectors;

public interface TypeInfoValidator {

    default List<String> validate(FeatureStoreTypeInfo typeInfo) {
        return typeInfo.getInstanceContainers()
                       .get(0)
                       .getAllAttributesContainers()
                       .stream()
                       .flatMap(attributesContainer -> validate(typeInfo.getName(), attributesContainer).stream())
                       .collect(Collectors.toList());
    }

    List<String> validate(String typeName, FeatureStoreAttributesContainer attributesContainer);

}
