/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.slick.javadsl.Slick;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.akka.http.ActorSystemProvider;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.query.api.FeatureConsumer;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.reflect.ClassTag$;
import slick.basic.DatabaseConfig;
import slick.basic.DatabaseConfig$;
import slick.jdbc.JdbcProfile;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.feature.provider.pgis.FeatureProviderPgis.PROVIDER_TYPE;
import static de.ii.xtraplatform.feature.query.api.TargetMapping.BASE_TYPE;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {@StaticServiceProperty(name = "providerType", type = "java.lang.String", value = PROVIDER_TYPE)})
public class FeatureProviderPgis implements TransformingFeatureProvider<FeatureTransformer, FeatureConsumer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderPgis.class);

    static final String PROVIDER_TYPE = "PGIS";

    private static final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
            .build());

    private final ActorSystem system;
    private final ActorMaterializer materializer;
    private final FeatureProviderDataPgis data;
    private SlickSession session;
    private Map<String, SqlFeatureSource> featureSources;
    private Map<String, SqlFeatureInserts> featureAddSinks;
    private Map<String, SqlFeatureInserts> featureUpdateSinks;
    private SqlFeatureRemover featureRemover;
    private Map<String, String> extentQueries;


    FeatureProviderPgis(@Context BundleContext context, @Requires ActorSystemProvider actorSystemProvider, @Requires CrsTransformation crsTransformation, @Property(name = ".data") FeatureProviderDataPgis data) {
        //TODO: starts akka for every instance, move to singleton
        this.system = actorSystemProvider.getActorSystem(context, config);
        this.materializer = ActorMaterializer.create(system);
        this.data = data;

        LOGGER.debug("CREATED PGIS: {}"/*, data*/);

        try {
            // bundle class loader has to be passed to Slick for initialization
            ClassLoader classLoader = context.getBundle()
                                             .adapt(BundleWiring.class)
                                             .getClassLoader();
            Thread.currentThread()
                  .setContextClassLoader(classLoader);
            DatabaseConfig<JdbcProfile> databaseConfig = DatabaseConfig$.MODULE$.forConfig("", createSlickConfig(data.getConnectionInfo()), classLoader, ClassTag$.MODULE$.apply(JdbcProfile.class));
            this.session = SlickSession.forConfig(databaseConfig);
            system.registerOnTermination(session::close);

            this.featureSources = createFeatureSources(data.getMappings());
            this.featureAddSinks = createFeatureSinks(data.getMappings(), false);
            this.featureUpdateSinks = createFeatureSinks(data.getMappings(), true);
            this.featureRemover = new SqlFeatureRemover(session, materializer);
            this.extentQueries = createExtentQueries(data.getMappings());
        } catch (Throwable e) {
            LOGGER.error("CONNECTING TO DB FAILED", e);
            this.session = null;
            this.featureSources = new LinkedHashMap<>();
        }
    }

    @Override
    public FeatureStream<FeatureConsumer> getFeatureStream(FeatureQuery query) {
        return featureConsumer -> createFeatureStream(query, featureConsumer);
    }

    @Override
    public FeatureStream<FeatureTransformer> getFeatureTransformStream(FeatureQuery query) {
        return featureTransformer -> createFeatureStream(query, new FeatureTransformerFromSql(data.getMappings()
                                                                                                  .get(query.getType()), featureTransformer, query.getFields(), query.skipGeometry()));
    }

    @Override
    public BoundingBox getSpatialExtent(String featureTypeId) {
        Optional<String> query = Optional.ofNullable(extentQueries.get(featureTypeId));

        BoundingBox[] boundingBox = {new BoundingBox(-180.0D, -90.0D, 180.0D, 90.0D, new EpsgCrs(4326))};

        if (!query.isPresent()) {
            return boundingBox[0];
        }

        Slick.source(session, query.get(), slickRow -> {
            BoundingBox boundingBox1 = parseBbox(slickRow.nextString());
            if (boundingBox1 != null) {
                boundingBox[0] = boundingBox1;
            }
            return slickRow;
        })
             .runWith(Sink.ignore(), materializer)
             .toCompletableFuture()
             .join();

        return boundingBox[0];
    }

    private BoundingBox parseBbox(String pgisBbox) {
        List<String> bbox = Splitter.onPattern("[(), ]")
                                    .omitEmptyStrings()
                                    .trimResults()
                                    .splitToList(pgisBbox);

        if (bbox.size() > 4) {
            return new BoundingBox(Double.parseDouble(bbox.get(1)), Double.parseDouble(bbox.get(2)), Double.parseDouble(bbox.get(3)), Double.parseDouble(bbox.get(4)), data.getNativeCrs());
        }

        return null;
    }

    @Override
    public List<String> addFeaturesFromStream(String featureType, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream) {
        Optional<SqlFeatureInserts> featureSink = Optional.ofNullable(featureAddSinks.get(featureType));

        if (!featureSink.isPresent()) {
            throw new NotFoundException("Feature type " + featureType + " not found");
        }

        //TODO: merge classes
        SqlFeatureCreator sqlFeatureCreator = new SqlFeatureCreator(session, materializer, featureSink.get());
        FeatureTransformerSql featureTransformerSql = new FeatureTransformerSql(sqlFeatureCreator, crsTransformer);

        try {
            stream.apply(featureTransformerSql)
                  .run(materializer)
                  .toCompletableFuture()
                  .join();

            return featureTransformerSql.getIds();
        } catch (CompletionException e) {
            if (e.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) e.getCause();
            } else if (e.getCause() instanceof JsonProcessingException) {
                throw new BadRequestException("Input could not be parsed", e.getCause());
            }
            LOGGER.error("Could not add feature", e.getCause());
            throw new BadRequestException("Feature not valid, could not be written");
        }
    }

    @Override
    public void updateFeatureFromStream(String featureType, String id, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream) {
        Optional<SqlFeatureInserts> featureSink = Optional.ofNullable(featureUpdateSinks.get(featureType));

        if (!featureSink.isPresent()) {
            throw new NotFoundException("Feature type " + featureType + " not found");
        }

       /* boolean removed = featureRemover.remove(featureType, id);
        if (!removed) {
            throw new NotFoundException();
        }*/

        //TODO: merge classes
        SqlFeatureCreator sqlFeatureCreator = new SqlFeatureCreator(session, materializer, featureSink.get());
        FeatureTransformerSql featureTransformerSql = new FeatureTransformerSql(sqlFeatureCreator, crsTransformer, id);

        try {
            stream.apply(featureTransformerSql)
                  .run(materializer)
                  .toCompletableFuture()
                  .join();

            List<String> ids = featureTransformerSql.getIds();
            LOGGER.debug("PUT {}", ids);
        } catch (CompletionException e) {
            if (e.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) e.getCause();
            } else if (e.getCause() instanceof JsonProcessingException) {
                throw new BadRequestException("Input could not be parsed", e.getCause());
            }
            LOGGER.error("Could not add feature", e.getCause());
            throw new BadRequestException("Feature not valid, could not be written");
        }
    }

    @Override
    public void deleteFeature(String featureType, String id) {
        featureRemover.remove(featureType, id, Optional.ofNullable(data.getTrigger())
                                                   .map(featureActionTrigger -> featureActionTrigger.getOnDelete(id))
                                                   .orElse(ImmutableList.of()));
    }

    @Override
    public Optional<String> encodeFeatureQuery(FeatureQuery query) {
        return Optional.empty();
    }

    @Override
    public String getSourceFormat() {
        return "application/sql";
    }

    private CompletionStage<Done> createFeatureStream(FeatureQuery query, FeatureConsumer featureConsumer) {
        Optional<SqlFeatureSource> featureSource = Optional.ofNullable(featureSources.get(query.getType()));

        if (!featureSource.isPresent()) {
            CompletableFuture<Done> promise = new CompletableFuture<>();
            promise.completeExceptionally(new IllegalStateException("No features available for type"));
            return promise;
        }

        return featureSource.get()
                            .runQuery(query, featureConsumer);
    }

    private Config createSlickConfig(ConnectionInfo connectionInfo) {
        String password = connectionInfo.getPassword();
        try {
            password = new String(Base64.getDecoder()
                                        .decode(password), Charsets.UTF_8);
        } catch (IllegalArgumentException e) {
            //ignore if not valid base64
        }

        return ConfigFactory.parseMap(ImmutableMap.<String, Object>builder()
                .put("profile", "slick.jdbc.PostgresProfile$")
                //.put("dataSourceClass", "org.postgresql.ds.PGSimpleDataSource")
                .put("db", ImmutableMap.<String, Object>builder()
                        .put("user", connectionInfo.getUser())
                        .put("password", password)
                        .put("dataSourceClass", "org.postgresql.ds.PGSimpleDataSource")
                        //.put("hikaricp.dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource")
                        //.put("hikaricp.datasource.user", connectionInfo.getUser())
                        //.put("hikaricp.datasource.password", connectionInfo.getPassword())
                        .put("properties.serverName", connectionInfo.getHost())
                        .put("properties.databaseName", connectionInfo.getDatabase())
                        //.put("url", String.format("jdbc:postgresql://%s/%s", connectionInfo.getHost(), connectionInfo.getDatabase()))
                        .put("numThreads", 10)
                        .put("initializationFailFast", true)
                        .build())
                .build());
    }

    private Map<String, SqlFeatureSource> createFeatureSources(Map<String, FeatureTypeMapping> mappings) {
        return mappings.entrySet()
                       .stream()
                       .map(featureTypeMappings -> {
                           Set<String> multiTables = new HashSet<>();

                           List<String> paths = featureTypeMappings.getValue()
                                                                   .getMappings()
                                                                   .entrySet()
                                                                   .stream()
                                                                   .sorted((stringSourcePathMappingEntry, t1) -> {
                                                                       SourcePathMapping value = t1.getValue();

                                                                       TargetMapping mappingForType = value.getMappingForType(TargetMapping.BASE_TYPE);

                                                                       Integer sortPriority = mappingForType.getSortPriority();

                                                                       SourcePathMapping value1 = stringSourcePathMappingEntry.getValue();

                                                                       TargetMapping mappingForType1 = value1.getMappingForType(TargetMapping.BASE_TYPE);

                                                                       Integer sortPriority1 = mappingForType1.getSortPriority();

                                                                       if (sortPriority1 == null) {
                                                                           return 1;
                                                                       }

                                                                       if (sortPriority == null) {
                                                                           return -1;
                                                                       }

                                                                       return sortPriority1 - sortPriority;
                                                                   })
                                                                   .peek(stringSourcePathMappingEntry -> {
                                                                       TargetMapping mapping = stringSourcePathMappingEntry.getValue()
                                                                                                                           .getMappingForType(TargetMapping.BASE_TYPE);

                                                                       if (mapping != null && mapping.getName() != null) {
                                                                           int i = mapping.getName()
                                                                                          .indexOf("[");
                                                                           while (i > -1) {
                                                                               multiTables.add(mapping.getName()
                                                                                                      .substring(i + 1, mapping.getName()
                                                                                                                               .indexOf("]", i)));
                                                                               i = mapping.getName()
                                                                                          .indexOf("[", i + 1);
                                                                           }
                                                                       }
                                                                   })
                                                                   .map(toPathWithGeomAsWkt())
                                                                   .collect(Collectors.toList());

                           //LOGGER.debug("PATHS {} {}", featureTypeMappings.getKey(), paths);

                           SqlPathTree sqlPathTree = new SqlPathTree.Builder()
                                   .fromPaths(paths)
                                   .build();

                           ImmutableSqlFeatureQueries queries = ImmutableSqlFeatureQueries.builder()
                                                                                          .paths(paths)
                                                                                          .multiTables(multiTables)
                                                                                          .sqlPaths(sqlPathTree)
                                                                                          .build();

                           SqlFeatureSource sqlFeatureSource = new SqlFeatureSource(session, queries, materializer, data.computeNumberMatched());

                           return new AbstractMap.SimpleImmutableEntry<>(featureTypeMappings.getKey(), sqlFeatureSource);
                       })
                       .collect(ImmutableMap.toImmutableMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue));
    }

    private Map<String, String> createExtentQueries(Map<String, FeatureTypeMapping> mappings) {
        return mappings.entrySet()
                       .stream()
                       .filter(featureTypeMappings -> featureTypeMappings.getValue().getMappings().entrySet().stream().anyMatch(stringSourcePathMappingEntry -> stringSourcePathMappingEntry.getValue()
                                                                                                                                                                                            .getMappingForType(BASE_TYPE)
                                                                                                                                                                                            .isSpatial()))
                       .map(featureTypeMappings -> {
                           Set<String> multiTables = new HashSet<>();

                           List<String> paths = featureTypeMappings.getValue()
                                                                   .getMappings()
                                                                   .entrySet()
                                                                   .stream()
                                                                   .filter(stringSourcePathMappingEntry -> stringSourcePathMappingEntry.getValue()
                                                                                                                                       .getMappingForType(BASE_TYPE)
                                                                                                                                       .isSpatial())
                                                                   .map(stringSourcePathMappingEntry -> geomToExtent(stringSourcePathMappingEntry.getKey()))
                                                                   .collect(Collectors.toList());

                           //LOGGER.debug("PATHS {} {}", featureTypeMappings.getKey(), paths);

                           SqlPathTree sqlPathTree = new SqlPathTree.Builder()
                                   .fromPaths(paths)
                                   .build();

                           ImmutableSqlFeatureQueries queries = ImmutableSqlFeatureQueries.builder()
                                                                                          .paths(paths)
                                                                                          .multiTables(multiTables)
                                                                                          .sqlPaths(sqlPathTree)
                                                                                          .build();


                           return new AbstractMap.SimpleImmutableEntry<>(featureTypeMappings.getKey(), queries.getQueries()
                                                                                                              .get(0)
                                                                                                              .toSqlSimple());
                       })
                       .collect(ImmutableMap.toImmutableMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue));
    }

    private Map<String, SqlFeatureInserts> createFeatureSinks(Map<String, FeatureTypeMapping> mappings, boolean withId) {
        return mappings.entrySet()
                       .stream()
                       .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), ImmutableSqlFeatureInserts.builder()
                                                                                                                      .withId(withId)
                                                                                                                      .sqlPaths(
                                                                                                                              new SqlPathTree.Builder()
                                                                                                                                      .fromPaths(
                                                                                                                                              entry.getValue()
                                                                                                                                                   .getMappings()
                                                                                                                                                   .entrySet()
                                                                                                                                                   .stream()
                                                                                                                                                   .map(toPathWithGeomAsWkt())
                                                                                                                                                   .collect(Collectors.toList())
                                                                                                                                      )
                                                                                                                                      .build()
                                                                                                                      )
                                                                                                                      .build()))
                       .collect(ImmutableMap.toImmutableMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue));
    }

    private Function<Map.Entry<String, SourcePathMapping>, String> toPathWithGeomAsWkt() {
        return mapping -> mapping.getValue()
                                 .getMappingForType(BASE_TYPE)
                                 .isSpatial() ? geomToWkt(mapping.getKey()) : mapping.getKey();
    }

    private String geomToWkt(String path) {
        int sep = path.lastIndexOf("/") + 1;
        return path.substring(0, sep) + "ST_AsText(ST_ForcePolygonCCW(" + path.substring(sep) + "))";
    }

    private String geomToExtent(String path) {
        int sep = path.lastIndexOf("/") + 1;
        return path.substring(0, sep) + "ST_Extent(" + path.substring(sep) + ")";
    }
}
