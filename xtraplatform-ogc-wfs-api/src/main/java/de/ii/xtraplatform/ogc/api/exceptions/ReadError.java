/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.ii.xtraplatform.ogc.api.exceptions;

import de.ii.xsf.core.api.exceptions.XtraserverFrameworkException;
import javax.ws.rs.core.Response;

/**
 *
 * @author fischer
 */
public class ReadError extends XtraserverFrameworkException {

    public ReadError(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public ReadError() {
        this.init();
    }

    private void init() {
        this.code = Response.Status.NOT_FOUND;
        this.htmlCode = this.code;
        this.stdmsg = "ReadError";
    }

    public ReadError(String msg) {
        this();
        this.msg = msg;
    }
}