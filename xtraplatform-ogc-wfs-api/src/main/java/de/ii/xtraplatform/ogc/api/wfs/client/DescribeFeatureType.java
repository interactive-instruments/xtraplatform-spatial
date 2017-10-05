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
package de.ii.xtraplatform.ogc.api.wfs.client;

import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author fischer
 */
public class DescribeFeatureType extends WFSOperationDescribeFeatureType {

    private Map<String, List<String>> fts;

    public DescribeFeatureType() {
    }
    
    public DescribeFeatureType(Map<String, List<String>> fts) {
        this.fts = fts;
    }

    @Override
    protected void initialize(XMLNamespaceNormalizer nsStore) {
    }

    @Override
    public Map<String, String> getGETParameters(XMLNamespaceNormalizer nsStore, Versions vs) {

        Map<String, String> params = new HashMap();
        
        params.put("REQUEST", this.getOperation().toString());
        params.put("SERVICE", "WFS");

        if (vs.getWfsVersion() != null) {
            params.put("VERSION", vs.getWfsVersion().toString());

            if (vs.getGmlVersion() != null && !vs.getGmlVersion().isGreaterOrEqual(vs.getWfsVersion().getGmlVersion())) {
                params.put("OUTPUTFORMAT", GML.getWord(vs.getGmlVersion(), GML.VOCABULARY.OUTPUTFORMAT_VALUE));
            }
        }

        if (this.fts != null && !fts.containsKey("")) {
            params.put("NAMESPACES", this.getNamespaceQueryParam(fts.keySet(), vs.getWfsVersion(), nsStore));
            params.put("TYPENAMES", this.getQualifiedNames(fts , nsStore));
        }

        return params;
    }
    
    private String getQualifiedNames(Map<String, List<String>> fts, XMLNamespaceNormalizer nsStore) {
        String names = "";
        int i = 0;
        for (Map.Entry<String, List<String>> ns : fts.entrySet()) {
            for (String localName : ns.getValue()) {
                if (i++ > 0) {
                    names += ",";
                }
                names += nsStore.getNamespacePrefix(ns.getKey()) + ":" + localName;
            }
        }
        return names;
    }
    
    
    private String getNamespaceQueryParam(Set<String> uri, WFS.VERSION wfsVersion, XMLNamespaceNormalizer nsStore) {
        String ns = "";

        if (wfsVersion == null) {
            return ns;
        }

        if (WFS.VERSION._2_0_0.compareTo(wfsVersion) >= 0) {
            Iterator<String> iter = uri.iterator();
            int i = 0;
            while (iter.hasNext()) {
                String u = iter.next();
                if (i++ > 0) {
                    ns += ",";
                }
                ns += "xmlns(" + nsStore.getNamespacePrefix(u) + "," + u + ")";
            }
        }
        return ns;
    }
    
}
