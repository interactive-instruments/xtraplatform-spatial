/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import com.google.common.collect.ImmutableMap;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class SqlInsert {

    protected abstract boolean getReturnId();
    protected abstract String getTable();
    protected abstract Map<String,String> getFieldsAndValues();

    //TODO: to builder???
    public void property(List<String> path, String value) {
        SqlFeatureQuery insert = getInserts().get(path.subList(0, path.size()-1));
        //insert.put(path.get(path.size()-1), value);
    }

    // for multiplicities
    public void row(List<String> path) {

    }

    // Map of Sub-Inserts, need id of parent, may have Sub-Inserts again
    @Value.Derived
    protected Map<List<String>, SqlFeatureQuery> getInserts() {
        return ImmutableMap.of();
    }
}
