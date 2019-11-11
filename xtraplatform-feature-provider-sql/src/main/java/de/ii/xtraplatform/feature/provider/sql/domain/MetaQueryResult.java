package de.ii.xtraplatform.feature.provider.sql.domain;

import org.immutables.value.Value;

@Value.Immutable
public interface MetaQueryResult {

    long getMinKey();

    long getMaxKey();

    long getNumberReturned();

    long getNumberMatched();

}
