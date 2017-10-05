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
package de.ii.ogc.wfs.proxy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zahnen
 */
public class WfsProxyCapabilities {

    private boolean paging;
    private Map<String, String> errorMessages;

    WfsProxyCapabilities() {
        this.errorMessages = new HashMap<>();
    }

    public boolean isPaging() {
        return paging;
    }

    public void setPaging(boolean paging) {
        this.paging = paging;
    }

    public Map<String, String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(Map<String, String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public void addErrorMessage(String capability, String message) {
        this.errorMessages.put(capability, message);
    }
}
