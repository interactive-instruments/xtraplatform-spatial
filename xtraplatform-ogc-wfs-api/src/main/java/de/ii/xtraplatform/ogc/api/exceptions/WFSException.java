package de.ii.xtraplatform.ogc.api.exceptions;

import de.ii.xsf.core.api.exceptions.XtraserverFrameworkException;
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
}
