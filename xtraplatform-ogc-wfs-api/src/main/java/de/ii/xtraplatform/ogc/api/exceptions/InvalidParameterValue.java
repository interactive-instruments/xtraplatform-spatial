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
