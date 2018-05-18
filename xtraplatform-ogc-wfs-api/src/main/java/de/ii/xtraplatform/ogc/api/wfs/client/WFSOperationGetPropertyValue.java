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

import de.ii.xtraplatform.ogc.api.WFS;

/**
 *
 * @author fischer
 */
  public abstract class WFSOperationGetPropertyValue extends WFSOperation {
        
    @Override
    public WFS.OPERATION getOperation() {
        return WFS.OPERATION.GET_PROPERTY_VALUE;
    }

    @Override
    protected String getOperationName(WFS.VERSION version) {
        return WFS.getWord(version, getOperation());
    }
}