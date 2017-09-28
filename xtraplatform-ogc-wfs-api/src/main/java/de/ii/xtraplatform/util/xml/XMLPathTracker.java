/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private XMLNamespaceNormalizer nsStore;
    
    private boolean multiple;

    public XMLPathTracker() {
        this.multiple = false;
        this.localPath = new ArrayList<>();
        this.path = new ArrayList<>();
    }
    
    public XMLPathTracker(XMLNamespaceNormalizer nsStore) {
        this();
        this.nsStore = nsStore;
    }
    
    public void track(String nsuri, String localName, int depth) {
        if (depth <= localPath.size()) {
            localPath.subList(depth - 1, localPath.size()).clear();
        }
        if (depth <= path.size()) {
            path.subList(depth - 1, path.size()).clear();
        }
        track(nsuri, localName);
    }
    
    public void track(String nsuri, String localName) {
        localPath.add(localName);
        if (nsuri != null && localName != null)
            path.add(nsuri + ":" + localName);
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
    
    @Override
    public String toString() {
        return Joiner.on("/").skipNulls().join(path);
    }
    
    public String toLocalPath() {
        return Joiner.on("/").skipNulls().join(localPath);
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
