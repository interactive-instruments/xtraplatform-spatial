package de.ii.xtraplatform.ogc.api.wfs.client;

import de.ii.xtraplatform.ogc.api.WFS;

/**
 *
 * @author fischer
 */
public abstract class WFSOperationGetFeature extends WFSOperation {
        
    @Override
    public WFS.OPERATION getOperation() {
        return WFS.OPERATION.GET_FEATURE;
    }

    @Override
    protected String getOperationName(WFS.VERSION version) {
        return WFS.getWord(version, WFS.VOCABULARY.GET_FEATURE);
    }
}
