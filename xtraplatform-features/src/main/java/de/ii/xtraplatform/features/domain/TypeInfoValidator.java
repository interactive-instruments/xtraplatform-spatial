package de.ii.xtraplatform.features.domain;

import java.util.List;

public interface TypeInfoValidator {

    List<String> validate(FeatureStoreAttributesContainer attributesContainer);

}
