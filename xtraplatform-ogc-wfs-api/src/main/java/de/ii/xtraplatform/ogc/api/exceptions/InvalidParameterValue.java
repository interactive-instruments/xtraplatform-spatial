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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.ii.xtraserver.framework.exceptions;

import de.ii.xsf.core.api.exceptions.XtraserverFrameworkException;
import javax.ws.rs.core.Response;

/**
 *
 * @author fischer
 */
public class InvalidParameterValue extends XtraserverFrameworkException {

    public InvalidParameterValue(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public InvalidParameterValue() {    
        this.init();
    }

    private void init() {
        this.code = Response.Status.BAD_REQUEST;
        this.stdmsg = "InvalidParameterValue";
    }
    
    public InvalidParameterValue(String msg) {
        this();
        this.msg = msg;
    }
    
}
