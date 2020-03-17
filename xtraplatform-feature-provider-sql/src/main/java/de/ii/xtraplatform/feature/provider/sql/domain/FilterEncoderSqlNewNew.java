package de.ii.xtraplatform.feature.provider.sql.domain;

import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FilterEncoder;

public interface FilterEncoderSqlNewNew extends FilterEncoder<String> {
    String encodeNested(CqlFilter cqlFilter, FeatureStoreAttributesContainer typeInfo, boolean isUserFilter);
}
