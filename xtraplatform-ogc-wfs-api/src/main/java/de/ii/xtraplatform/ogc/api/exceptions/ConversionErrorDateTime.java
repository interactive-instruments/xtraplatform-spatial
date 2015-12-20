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
public class ConversionErrorDateTime extends XtraserverFrameworkException {

    public ConversionErrorDateTime(Object m, Object... args) {
        super(m, args);
        this.init();
    }

    public ConversionErrorDateTime() {
        this.init();
    }

    private void init() {
        this.code = Response.Status.INTERNAL_SERVER_ERROR;
        this.stdmsg = "ConversionErrorDateTime";
    }

    public ConversionErrorDateTime(String msg) {
        this();
        this.msg = msg;
    }
}
