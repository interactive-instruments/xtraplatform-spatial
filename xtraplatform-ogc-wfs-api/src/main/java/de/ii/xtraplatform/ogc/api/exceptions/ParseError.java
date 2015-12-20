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
public class ParseError extends XtraserverFrameworkException {

    public ParseError(Object m, Object... args) {
        super(m, args);
        this.init();
    }

    public ParseError() {
        this.init();
    }

    private void init() {
        this.code = Response.Status.NOT_FOUND;
        this.htmlCode = Response.Status.NOT_FOUND;
        this.stdmsg = "ParseError";
    }

    public ParseError(String msg) {
        this();
        this.msg = msg;
    }
}
