package de.ii.xtraplatform.feature.provider.sql.domain;

import org.immutables.value.Value;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.OptionalLong;

@Value.Immutable
public interface SqlRowMeta extends SqlRow {

    long getMinKey();

    long getMaxKey();

    long getNumberReturned();

    OptionalLong getNumberMatched();

    @Override
    default int compareTo(SqlRow row) {
        return -1;
    }
}
