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
        return WFS.getWord(version, getOperation());
    }
    
}
