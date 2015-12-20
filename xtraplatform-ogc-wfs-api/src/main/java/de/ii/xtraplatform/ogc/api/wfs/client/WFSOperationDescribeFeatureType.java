/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.ii.xtraplatform.ogc.api.wfs.client;

import de.ii.xtraplatform.ogc.api.WFS;

/**
 *
 * @author fischer
 */
public abstract class WFSOperationDescribeFeatureType  extends WFSOperation {

    @Override
    public WFS.OPERATION getOperation() {
        return WFS.OPERATION.DESCRIBE_FEATURE_TYPE;
    }

    @Override
    protected String getOperationName(WFS.VERSION version) {
        return WFS.getWord(version, WFS.VOCABULARY.DESCRIBE_FEATURE_TYPE);
    }
    
}
