package de.ii.xtraplatform.feature.provider.api;

import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
public interface FeatureProperty {

    enum Type {
        INTEGER,
        FLOAT,
        STRING,
        BOOLEAN,
        DATETIME,
        GEOMETRY
    }

    String getName();

    Type getType();

    Map<String,String> getAdditionalInfo();
}
