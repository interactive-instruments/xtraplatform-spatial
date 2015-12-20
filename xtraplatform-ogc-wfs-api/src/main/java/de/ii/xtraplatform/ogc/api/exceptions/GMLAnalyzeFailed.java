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
public class GMLAnalyzeFailed extends XtraserverFrameworkException {

    public GMLAnalyzeFailed(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public GMLAnalyzeFailed() {
        this.init();
    }

    private void init() {
        this.code = Response.Status.INTERNAL_SERVER_ERROR;
        this.stdmsg = "GMLAnalyzeFailed";
    }

    public GMLAnalyzeFailed(String msg) {
        this();
        this.msg = msg;
    }
}
