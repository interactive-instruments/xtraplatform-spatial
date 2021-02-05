/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.OptionalLong;

@Value.Immutable
public interface SqlRowMeta extends SqlRow {

    @Nullable
    Object getMinKey();

    @Nullable
    Object getMaxKey();

    long getNumberReturned();

    OptionalLong getNumberMatched();

    @Override
    default int compareTo(SqlRow row) {
        return -1;
    }
}
