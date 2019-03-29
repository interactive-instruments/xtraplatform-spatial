/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.api.exceptions;

import de.ii.xtraplatform.api.exceptions.XtraserverFrameworkException;
import javax.ws.rs.core.Response;

/**
 *
 * @author fischer
 */
public class RequestNotSupported extends XtraserverFrameworkException {

    public RequestNotSupported(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public RequestNotSupported() {
        this.init();
    }

    private void init() {
        this.code = Response.Status.NOT_FOUND;
        this.htmlCode = this.code;
        this.stdmsg = "RequestNotSupported";
    }

    public RequestNotSupported(String msg) {
        this();
        this.msg = msg;
    }
}
