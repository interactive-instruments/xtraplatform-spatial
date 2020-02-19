package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityComponent;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

//@Component
//@Provides(properties = {@StaticServiceProperty(name = "providerType", type = "java.lang.String", value = FeatureProviderSql.PROVIDER_TYPE)})
@EntityComponent
@Entity(entityType = FeatureProvider2.class, dataType = FeatureProviderDataV1.class, type = "providers", subType = "feature/sql")
public class FeatureProviderSql extends AbstractFeatureProvider<SqlRow, SqlQueries, SqlQueryOptions> implements FeatureProvider2, FeatureQueries, FeatureExtents, FeatureCrs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderSql.class);

    private final CrsTransformerFactory crsTransformerFactory;
    private final SqlConnector connector;
    private final FeatureStoreQueryGeneratorSql queryGeneratorSql;
    private final FeatureQueryTransformerSql queryTransformer;
    private final FeatureNormalizerSql featureNormalizer;
    private final ExtentReader extentReader;

    public FeatureProviderSql(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Property(name = "data") FeatureProviderDataV1 data,
                              @Property(name = ".connector") SqlConnector sqlConnector) {
        //TODO: starts akka for every instance, move to singleton
        super(context, actorSystemProvider, data, createPathParser((ConnectionInfoSql) data.getConnectionInfo()));

        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = sqlConnector;
        //TODO: from config
        SqlDialect sqlDialect = new SqlDialectPostGis();
        this.queryGeneratorSql = new FeatureStoreQueryGeneratorSql(sqlDialect, data.getNativeCrs());
        this.queryTransformer = new FeatureQueryTransformerSql(getTypeInfos(), queryGeneratorSql, ((ConnectionInfoSql) data.getConnectionInfo()).getComputeNumberMatched());
        this.featureNormalizer = new FeatureNormalizerSql(getTypeInfos(), data.getTypes());
        this.extentReader = new ExtentReaderSql(connector, queryGeneratorSql, sqlDialect, data.getNativeCrs());
    }

    private static FeatureStorePathParser createPathParser(ConnectionInfoSql connectionInfoSql) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(connectionInfoSql.getPathSyntax())
                                                     .build();
        return new FeatureStorePathParserSql(syntax);
    }

    @Override
    public FeatureProviderDataV1 getData() {
        return super.getData();
    }

    @Override
    protected FeatureQueryTransformer<SqlQueries> getQueryTransformer() {
        return queryTransformer;
    }

    @Override
    protected FeatureProviderConnector<SqlRow, SqlQueries, SqlQueryOptions> getConnector() {
        return connector;
    }

    @Override
    protected FeatureNormalizer<SqlRow> getNormalizer() {
        return featureNormalizer;
    }

    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        return getData().getNativeCrs()
                        .equals(crs) || crsTransformerFactory.isCrsSupported(crs);
    }

    @Override
    public boolean is3dSupported() {
        return crsTransformerFactory.isCrs3d(getData().getNativeCrs());
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String typeName) {
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

        if (!typeInfo.isPresent()) {
            return Optional.empty();
        }

        return extentReader.getExtent(typeInfo.get())
                           .run(getMaterializer())
                           .exceptionally(throwable -> Optional.empty())
                           .toCompletableFuture()
                           .join();
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String typeName, EpsgCrs crs) {
        return getSpatialExtent(typeName).flatMap(boundingBox -> crsTransformerFactory.getTransformer(getData().getNativeCrs(), crs)
                                                                                      .flatMap(crsTransformer -> {
                                                                                          try {
                                                                                              return Optional.of(crsTransformer.transformBoundingBox(boundingBox));
                                                                                          } catch (CrsTransformationException e) {
                                                                                              return Optional.empty();
                                                                                          }
                                                                                      }));
    }
}
