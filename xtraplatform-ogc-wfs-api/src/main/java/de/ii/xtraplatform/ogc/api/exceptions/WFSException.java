/**
 * Copyright 2020 interactive instruments GmbH
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
public class WFSException extends XtraserverFrameworkException {

    public WFSException(Object m, Object... args) {
        super(m, args);
        this.init();
    }

    public WFSException() {
        super();
        this.init();
    }

    public WFSException(Throwable cause) {
        super(cause);
        this.init();
    }

    private void init() {
        this.code = Response.Status.INTERNAL_SERVER_ERROR;
        this.htmlCode = Response.Status.INTERNAL_SERVER_ERROR;
        this.stdmsg = "WFSException";  
    }

    public WFSException(String msg) {
        this();
        this.msg = msg;
    }

    public WFSException(String msg, Throwable cause) {
        this(cause);
        this.msg = msg;
    }
}
