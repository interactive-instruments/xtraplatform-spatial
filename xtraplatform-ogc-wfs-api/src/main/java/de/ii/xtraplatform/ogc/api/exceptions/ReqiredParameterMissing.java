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
public class ReqiredParameterMissing extends XtraserverFrameworkException {

    public ReqiredParameterMissing(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public ReqiredParameterMissing() {
             this.init();
    }

    private void init() {
        this.code = Response.Status.BAD_REQUEST;
        this.stdmsg = "ReqiredParameterMissing";
    }

    public ReqiredParameterMissing(String msg) {
        this();
        this.msg = msg;
    }

    public ReqiredParameterMissing(String msg, String callback) {
        this();
        this.msg = msg;
        this.callback = callback;
    }
}