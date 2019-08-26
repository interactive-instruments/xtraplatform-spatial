/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.feature.transformer.geojson;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author zahnen
 */
// TODO: make XMLPathTracker extension of this
public class JsonPathTracker {

    private final String[] localPath;
    private int length;
    private static final Joiner JOINER = Joiner.on('.')
                                               .skipNulls();

    public JsonPathTracker() {
        this.localPath = new String[64];
        this.length = 0;
    }

    public void track(int depth) {
        shorten(depth);
    }

    public void track(String localName, int depth) {
        if (depth < 0) {
            return;
        }
        shorten(depth);

        track(localName);
    }

    private void shorten(final int depth) {
        if (depth <= 0) {
            return;
        }
        if (depth <= length/*localPath.size()*/) {
            //localPath.subList(depth - 1, localPath.size()).clear();
            length = depth - 1;
        }
    }

    public void track(String localName) {
        localPath[length] = localName;
        length++;
    }

    @Override
    public String toString() {
        //return JOINER.join(localPath.build());
        if (length == 0) return "";
        return Arrays.stream(localPath, 0, length - 1)
                     .collect(Collectors.joining("."));
    }

    public List<String> asList() {
        if (length == 0) return ImmutableList.of();
        return Arrays.stream(localPath, 0, length - 1)
                     .collect(ImmutableList.toImmutableList());
    }
}
