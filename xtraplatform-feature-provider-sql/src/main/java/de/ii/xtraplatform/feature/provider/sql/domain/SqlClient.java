package de.ii.xtraplatform.feature.provider.sql.domain;

import akka.NotUsed;
import akka.stream.javadsl.Source;

public interface SqlClient {

    Source<SqlRow, NotUsed> getSourceStream(String query, SqlQueryOptions options);

}
