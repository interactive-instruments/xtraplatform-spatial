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
public class SchemaParseException extends XtraserverFrameworkException {

    public SchemaParseException(Object m, Object... args) {
        super(m, args);
        this.init();
    }

    public SchemaParseException() {
        this.init();
    }

    private void init() {
        this.code = Response.Status.INTERNAL_SERVER_ERROR;
        this.htmlCode = this.code;
        this.stdmsg = "SchemaParseException";
    }

    public SchemaParseException(String msg) {
        this();
        this.msg = msg;
    }
}
