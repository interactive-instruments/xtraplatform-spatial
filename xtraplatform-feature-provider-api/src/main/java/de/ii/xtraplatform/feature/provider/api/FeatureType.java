package de.ii.xtraplatform.feature.provider.api;

import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

@Value.Immutable
public interface FeatureType {

    String getName();

    List<FeatureProperty> getProperties();

    Map<String,String> getAdditionalInfo();

}
