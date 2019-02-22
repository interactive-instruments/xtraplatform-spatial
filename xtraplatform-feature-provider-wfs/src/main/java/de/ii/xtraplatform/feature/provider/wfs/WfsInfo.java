package de.ii.xtraplatform.feature.provider.wfs;

import java.util.Map;

/**
 * @author zahnen
 */
public abstract class WfsInfo {
    public abstract String getVersion();
    public abstract String getGmlVersion();
    public abstract Map<String,String> getNamespaces();
}
