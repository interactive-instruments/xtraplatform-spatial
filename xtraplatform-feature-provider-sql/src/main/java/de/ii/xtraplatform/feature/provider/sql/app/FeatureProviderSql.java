/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
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

import java.util.Objects;
import java.util.Optional;

//@Component
//@Provides(properties = {@StaticServiceProperty(name = "providerType", type = "java.lang.String", value = FeatureProviderSql.PROVIDER_TYPE)})
@EntityComponent
@Entity(type = FeatureProvider2.ENTITY_TYPE, subType = FeatureProviderSql.ENTITY_SUB_TYPE, dataClass = FeatureProviderDataV1.class)
public class FeatureProviderSql extends AbstractFeatureProvider<SqlRow, SqlQueries, SqlQueryOptions> implements FeatureProvider2, FeatureQueries, FeatureExtents, FeatureCrs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderSql.class);

    static final String ENTITY_SUB_TYPE = "feature/sql";
    public static final String PROVIDER_TYPE = "SQL";

    private final CrsTransformerFactory crsTransformerFactory;
    private final SqlConnector connector;
    private final FeatureStoreQueryGeneratorSql queryGeneratorSql;
    private final FeatureQueryTransformerSql queryTransformer;
    private final FeatureNormalizerSql featureNormalizer;
    private final ExtentReader extentReader;

    public FeatureProviderSql(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Requires Cql cql,
                              @Property(name = "data") FeatureProviderDataV1 data,
                              @Property(name = ".connector") SqlConnector sqlConnector) {
        //TODO: starts akka for every instance, move to singleton
        super(context, actorSystemProvider, data, createPathParser((ConnectionInfoSql) data.getConnectionInfo(), cql));

        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = sqlConnector;
        //TODO: from config
        SqlDialect sqlDialect = new SqlDialectPostGis();
        this.queryGeneratorSql = new FeatureStoreQueryGeneratorSql(sqlDialect, data.getNativeCrs().orElse(OgcCrs.CRS84),crsTransformerFactory);
        this.queryTransformer = new FeatureQueryTransformerSql(getTypeInfos(), queryGeneratorSql, ((ConnectionInfoSql) data.getConnectionInfo()).getComputeNumberMatched());
        this.featureNormalizer = new FeatureNormalizerSql(getTypeInfos(), data.getTypes());
        this.extentReader = new ExtentReaderSql(connector, queryGeneratorSql, sqlDialect, data.getNativeCrs().orElse(OgcCrs.CRS84));
    }

    private static FeatureStorePathParser createPathParser(ConnectionInfoSql connectionInfoSql, Cql cql) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(connectionInfoSql.getPathSyntax())
                                                     .build();
        return new FeatureStorePathParserSql(syntax, cql);
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
    public boolean supportsCrs() {
        return FeatureProvider2.super.supportsCrs() && getData().getNativeCrs().isPresent();
    }

    @Override
    public EpsgCrs getNativeCrs() {
        return getData().getNativeCrs().get();
    }

    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        return Objects.equals(getNativeCrs(), crs) || crsTransformerFactory.isCrsSupported(crs);
    }

    @Override
    public boolean is3dSupported() {
        return crsTransformerFactory.isCrs3d(getNativeCrs());
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String typeName) {
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

        if (!typeInfo.isPresent()) {
            return Optional.empty();
        }

        try {
            return extentReader.getExtent(typeInfo.get())
                               .run(getMaterializer())
                               .exceptionally(throwable -> Optional.empty())
                               .toCompletableFuture()
                               .join();
        } catch (Throwable e) {
            //continue
        }

        return Optional.empty();
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String typeName, EpsgCrs crs) {
        return getSpatialExtent(typeName).flatMap(boundingBox -> crsTransformerFactory.getTransformer(getNativeCrs(), crs)
                                                                                      .flatMap(crsTransformer -> {
                                                                                          try {
                                                                                              return Optional.of(crsTransformer.transformBoundingBox(boundingBox));
                                                                                          } catch (CrsTransformationException e) {
                                                                                              return Optional.empty();
                                                                                          }
                                                                                      }));
    }
}
