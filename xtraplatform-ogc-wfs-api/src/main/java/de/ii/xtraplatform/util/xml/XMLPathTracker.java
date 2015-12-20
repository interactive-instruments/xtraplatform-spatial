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
        this.localPath = new ArrayList();
        this.path = new ArrayList();
    }
    
    public XMLPathTracker(XMLNamespaceNormalizer nsStore) {
        this();
        this.nsStore = nsStore;
    }
    
    public void track(String nsuri, String localName, int depth) {
        if (depth <= path.size()) {
            localPath.subList(depth - 1, localPath.size()).clear();
            path.subList(depth - 1, path.size()).clear();
        }
        track(nsuri, localName);
    }
    
    public void track(String nsuri, String localName) {
        localPath.add(localName);
        path.add(nsuri + ":" + localName);
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }
    
    public String toFieldName() {
        return Joiner.on(".").join(localPath);
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
