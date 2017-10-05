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
package de.ii.xtraplatform.util.xml;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zahnen
 */
public class XMLPathTracker {
    private List<String> localPath;
    private List<String> path;
    private List<String> noObjectPath;
    private XMLNamespaceNormalizer nsStore;

    private boolean multiple;

    public XMLPathTracker() {
        this.multiple = false;
        this.localPath = new ArrayList<>();
        this.path = new ArrayList<>();
        this.noObjectPath = new ArrayList<>();
    }

    public XMLPathTracker(XMLNamespaceNormalizer nsStore) {
        this();
        this.nsStore = nsStore;
    }

    public void track(String nsuri, String localName, int depth) {
        if (depth <= path.size()) {
            localPath.subList(depth - 1, localPath.size()).clear();
            path.subList(depth - 1, path.size()).clear();
            noObjectPath.subList(depth - 1, noObjectPath.size()).clear();
        }
        track(nsuri, localName);
    }

    public void track(String nsuri, String localName) {
        localPath.add(localName);
        if (nsuri != null && localName != null)
            path.add(nsuri + ":" + localName);
        if (localName != null && !Character.isUpperCase(localName.charAt(0))) {
            noObjectPath.add(localName);
        } else {
            noObjectPath.add(null);
        }
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public String toFieldName() {
        return Joiner.on(".").skipNulls().join(localPath);
    }

    public String toFieldNameGml() {
        return Joiner.on(".").skipNulls().join(noObjectPath);
    }

    @Override
    public String toString() {
        return Joiner.on("/").join(path);
    }

    public String toLocalPath() {
        return Joiner.on("/").join(localPath);
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    public void clear() {
        localPath.clear();
        path.clear();
        this.multiple = false;
    }
}
