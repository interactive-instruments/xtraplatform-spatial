package de.ii.xtraplatform.feature.provider.sql;

import org.immutables.value.Value;

@Value.Immutable
public interface SqlPathColumn {

    @Value.Derived
    default String getName() {
        return getPath().substring(getPath().lastIndexOf("/") + 1);
    }

    String getPath();
}
