/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import akka.Done;
import akka.stream.javadsl.RunnableGraph;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureStream;
import de.ii.xtraplatform.scheduler.api.TaskProgress;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * @author zahnen
 */
public interface TransformingFeatureProvider<T extends FeatureTransformer, U extends FeatureConsumer> extends FeatureProvider<U> {
        FeatureStream<T> getFeatureTransformStream(FeatureQuery query);

    //TODO
        List<String> addFeaturesFromStream(String featureType, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream);
        void updateFeatureFromStream(String featureType, String id, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream);
        void deleteFeature(String featureType, String id);

    boolean supportsCrs(EpsgCrs crs);

    default boolean shouldSwapCoordinates(EpsgCrs crs) {
        return false;
    }

    interface SchemaAware {
                void getSchema(FeatureProviderSchemaConsumer schemaConsumer, Map<String, QName> featureTypes, TaskProgress taskProgress);
        }

        interface DataGenerator {
                FeatureProviderMetadataConsumer getDataGenerator(FeatureProviderDataTransformer data,
                                                                 ImmutableFeatureProviderDataTransformer.Builder dataBuilder);

                FeatureProviderSchemaConsumer getMappingGenerator(
                        FeatureProviderDataTransformer data,
                        ImmutableFeatureProviderDataTransformer.Builder dataBuilder,
                        List<TargetMappingProviderFromGml> mappingProviders);
        }
}
