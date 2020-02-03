package de.ii.xtraplatform.feature.provider.sql.app;

import akka.NotUsed;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.geometries.domain.BoundingBox;
import de.ii.xtraplatform.geometries.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

class ExtentReaderSql {

    private final SqlConnector sqlConnector;
    private final FeatureStoreQueryGeneratorSql queryGenerator;
    private final SqlDialect sqlDialect;
    private final EpsgCrs crs;

    ExtentReaderSql(SqlConnector sqlConnector, FeatureStoreQueryGeneratorSql queryGenerator,
                    SqlDialect sqlDialect, EpsgCrs crs) {
        this.sqlConnector = sqlConnector;
        this.queryGenerator = queryGenerator;
        this.sqlDialect = sqlDialect;
        this.crs = crs;
    }


    public RunnableGraph<CompletionStage<Optional<BoundingBox>>> getExtent(FeatureStoreTypeInfo typeInfo) {
        //TODO: multiple main tables
        FeatureStoreInstanceContainer instanceContainer = typeInfo.getInstanceContainers()
                                                                  .get(0);
        Optional<FeatureStoreAttributesContainer> spatialAttributesContainer = instanceContainer.getSpatialAttributesContainer();

        if (!spatialAttributesContainer.isPresent()) {
            throw new IllegalArgumentException("feature type has no geometry:" + typeInfo.getName());
        }

        String query = queryGenerator.getExtentQuery(spatialAttributesContainer.get());

        Source<SqlRow, NotUsed> sourceStream = sqlConnector.getSqlClient().getSourceStream(query, SqlQueryOptions.withColumnTypes(String.class));

        return sourceStream.map(sqlRow -> sqlDialect.parseExtent((String) sqlRow.getValues()
                                                                                .get(0), crs))
                           .toMat(Sink.head(), Keep.right());
    }
}
