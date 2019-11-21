package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.feature.provider.api.FeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderSchemaConsumer;

import java.util.List;

//TODO
public interface FeatureProviderGenerator {
            FeatureProviderMetadataConsumer getDataGenerator(FeatureProviderDataTransformer data,
                                                             ImmutableFeatureProviderDataTransformer.Builder dataBuilder);

            FeatureProviderSchemaConsumer getMappingGenerator(
                    FeatureProviderDataTransformer data,
                    ImmutableFeatureProviderDataTransformer.Builder dataBuilder,
                    List<TargetMappingProviderFromGml> mappingProviders);
    }
