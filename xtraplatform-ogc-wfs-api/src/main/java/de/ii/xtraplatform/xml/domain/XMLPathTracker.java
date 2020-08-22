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
package de.ii.xtraplatform.xml.domain;

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
    private final Joiner joiner;
    private final Joiner dotJoiner;
    private final StringBuilder stringBuilder;

    private boolean multiple;

    public XMLPathTracker() {
        this.multiple = false;
        this.localPath = new ArrayList<>();
        this.path = new ArrayList<>();
        this.noObjectPath = new ArrayList<>();
        this.joiner = Joiner.on('/').skipNulls();
        this.dotJoiner = Joiner.on('.').skipNulls();
        this.stringBuilder = new StringBuilder();
    }

    public XMLPathTracker(XMLNamespaceNormalizer nsStore) {
        this();
        this.nsStore = nsStore;
    }

    public void track(int depth) {
        shorten(depth);
    }

    public void track(String nsuri, String localName, int depth, boolean isMultiple) {
        if (depth < 0) {
            return;
        }
        shorten(depth);

        track(nsuri, localName, isMultiple);
    }

    private void shorten(final int depth) {
        if (depth <= 0) {
            return;
        }
        if (depth <= localPath.size()) {
            localPath.subList(depth - 1, localPath.size()).clear();
        }
        if (depth <= path.size()) {
            path.subList(depth - 1, path.size()).clear();
            noObjectPath.subList(depth - 1, noObjectPath.size()).clear();
        }
    }

    public void track(String nsuri, String localName, boolean isMultiple) {
        localPath.add(localName);
        if (nsuri != null && localName != null)
            path.add(nsuri + ":" + localName);
        if (localName != null && (!Character.isUpperCase(localName.charAt(0)) || noObjectPath.isEmpty())) {
            noObjectPath.add(localName + (isMultiple ? "[]" : ""));
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
        return joiner.join(localPath);
    }

    public String toFieldNameGml() {
        return dotJoiner.join(noObjectPath);
    }

    @Override
    public String toString() {
        stringBuilder.setLength(0);
        return joiner.appendTo(stringBuilder, path).toString();
    }

    public String toLocalPath() {
        return joiner.join(localPath);
    }

    public List<String> asList() {
        return path;
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
