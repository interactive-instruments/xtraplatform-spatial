/**
 * Copyright 2017 European Union, interactive instruments GmbH
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
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zahnen
 */
// TODO: make XMLPathTracker extension of this
public class JsonPathTracker {
    private List<String> localPath;
    private final Joiner joiner;

    public JsonPathTracker() {
        this.localPath = new ArrayList<>();
        this.joiner = Joiner.on('.').skipNulls();
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
        if (depth <= localPath.size()) {
            localPath.subList(depth - 1, localPath.size()).clear();
        }
    }

    public void track(String localName) {
        localPath.add(localName);
    }

    @Override
    public String toString() {
        return joiner.join(localPath);
    }

    public List<String> asList() {
        return localPath;
    }
}
