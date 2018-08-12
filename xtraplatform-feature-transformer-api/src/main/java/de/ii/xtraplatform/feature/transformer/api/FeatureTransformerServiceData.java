package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.query.api.FeatureProviderData;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.service.api.ServiceData;
import org.immutables.value.Value;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.feature.query.api.TargetMapping.BASE_TYPE;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableFeatureTransformerServiceData.class)
public abstract class FeatureTransformerServiceData extends ServiceData {

    //TODO
    public abstract Map<String, FeatureTypeConfigurationWfs3> getFeatureTypes();

    public abstract FeatureProviderDataTransformer getFeatureProvider();
}
