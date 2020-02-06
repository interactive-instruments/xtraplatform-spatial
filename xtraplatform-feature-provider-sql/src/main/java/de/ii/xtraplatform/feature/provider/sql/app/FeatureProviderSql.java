package de.ii.xtraplatform.feature.provider.sql.app;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityComponent;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.SqlFeatureTypeParser;
import de.ii.xtraplatform.feature.provider.sql.SqlMappingParser;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreTypeInfo;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//@Component
//@Provides(properties = {@StaticServiceProperty(name = "providerType", type = "java.lang.String", value = FeatureProviderSql.PROVIDER_TYPE)})
@EntityComponent
@Entity(entityType = FeatureProvider2.class, dataType = FeatureProviderDataV1.class, type = "providers")
public class FeatureProviderSql extends AbstractFeatureProvider<SqlRow, SqlQueries, SqlQueryOptions> implements FeatureProvider2, FeatureQueries, FeatureExtents, FeatureCrs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderSql.class);

    private static final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
            .build());

    private final ActorSystem system;
    private final ActorMaterializer materializer;
    private final CrsTransformerFactory crsTransformerFactory;
    private final SqlConnector connector;
    private final FeatureStorePathParser pathParser;
    private final Map<String, FeatureStoreTypeInfo> typeInfos;
    private final FeatureStoreQueryGeneratorSql queryGeneratorSql;
    private final FeatureQueryTransformerSql queryTransformer;
    private final FeatureNormalizerSql featureNormalizer;
    private final ExtentReaderSql extentReader;

    public FeatureProviderSql(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Property(name = "data") FeatureProviderDataV1 data,
                              @Property(name = ".connector") SqlConnector sqlConnector) {
        //TODO: starts akka for every instance, move to singleton
        super(null);
        this.system = actorSystemProvider.getActorSystem(context, config);
        this.materializer = ActorMaterializer.create(system);
        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = sqlConnector;

        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(((ConnectionInfoSql)data.getConnectionInfo()).getPathSyntax())
                                                     .build();
        //TODO: merge
        SqlFeatureTypeParser mappingParser = new SqlFeatureTypeParser(syntax);
        this.pathParser = new FeatureStorePathParserSql(syntax);

        this.typeInfos = getTypeInfos2(data.getTypes(), ((ConnectionInfoSql)data.getConnectionInfo()).getPathSyntax());
        //TODO: from config
        SqlDialect sqlDialect = new SqlDialectPostGis();
        this.queryGeneratorSql = new FeatureStoreQueryGeneratorSql(new FilterEncoderSqlNewImpl(data.getNativeCrs()), sqlDialect);
        this.queryTransformer = new FeatureQueryTransformerSql(typeInfos, queryGeneratorSql, false/*TODO data.computeNumberMatched()*/);
        this.featureNormalizer = new FeatureNormalizerSql(typeInfos, data.getTypes());
        this.extentReader = new ExtentReaderSql(connector, queryGeneratorSql, sqlDialect, data.getNativeCrs());
    }

    @Override
    protected FeatureStorePathParser getPathParser() {
        return pathParser;
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

    //TODO: from data.getTypes()
    //TODO: move to derived in data?
    private Map<String, FeatureStoreTypeInfo> getTypeInfos(Map<String, FeatureTypeMapping> mappings) {
        //TODO: options from data
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .build();
        SqlMappingParser mappingParser = new SqlMappingParser(syntax);
        FeatureStorePathParser pathParser = new FeatureStorePathParserSql(syntax);

        return mappings.entrySet()
                       .stream()
                       .map(entry -> {
                           List<String> paths = mappingParser.parse(entry.getValue()
                                                                         .getMappings());
                           List<FeatureStoreInstanceContainer> instanceContainers = pathParser.parse(paths);
                           FeatureStoreTypeInfo typeInfo = ImmutableFeatureStoreTypeInfo.builder()
                                                                                        .name(entry.getKey())
                                                                                        .instanceContainers(instanceContainers)
                                                                                        .build();

                           return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), typeInfo);
                       })
                       .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, FeatureStoreTypeInfo> getTypeInfos2(Map<String, FeatureType> featureTypes, SqlPathSyntax.Options syntaxOptions) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(syntaxOptions)
                                                     .build();
        SqlFeatureTypeParser mappingParser = new SqlFeatureTypeParser(syntax);
        FeatureStorePathParser pathParser = new FeatureStorePathParserSql(syntax);

        return featureTypes.entrySet()
                           .stream()
                           .map(entry -> {
                               List<String> paths = mappingParser.parse(entry.getValue());
                               List<FeatureStoreInstanceContainer> instanceContainers = pathParser.parse(paths);
                               FeatureStoreTypeInfo typeInfo = ImmutableFeatureStoreTypeInfo.builder()
                                                                                            .name(entry.getKey())
                                                                                            .instanceContainers(instanceContainers)
                                                                                            .build();

                               return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), typeInfo);
                           })
                           .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    //TODO: crs as second param, transform here? yup
    @Override
    public BoundingBox getSpatialExtent(String typeName) {
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(typeInfos.get(typeName));
        //TODO: immutable
        //TODO: really provide fallback, or better move to caller?
        //TODO: has to be in native crs, otherwise it will not work
        BoundingBox boundingBoxFallback = new BoundingBox(-180.0D, -90.0D, 180.0D, 90.0D, EpsgCrs.of(4326));

        if (!typeInfo.isPresent()) {
            return boundingBoxFallback;
        }

        return extentReader.getExtent(typeInfo.get())
                           .run(materializer)
                           .exceptionally(throwable -> Optional.of(boundingBoxFallback))
                           .toCompletableFuture()
                           .join()
                           .orElse(boundingBoxFallback);
    }
}
