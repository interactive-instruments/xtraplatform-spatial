package de.ii.xtraplatform.feature.provider.sql.domain;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;

public interface FeatureReaderSql {

    Source<SqlRow, NotUsed> getRowStream(FeatureQuery query, FeatureStoreTypeInfo typeInfo, boolean computeNumberMatched);
}
