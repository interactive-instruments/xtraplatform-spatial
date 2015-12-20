package de.ii.xtraserver.framework.exceptions;

import de.ii.xsf.core.api.exceptions.XtraserverFrameworkException;
import javax.ws.rs.core.Response;

/**
 *
 * @author fischer
 */
public class RequestNotSupported extends XtraserverFrameworkException {

    public RequestNotSupported(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public RequestNotSupported() {
        this.init();
    }

    private void init() {
        this.code = Response.Status.NOT_FOUND;
        this.htmlCode = this.code;
        this.stdmsg = "RequestNotSupported";
    }

    public RequestNotSupported(String msg) {
        this();
        this.msg = msg;
    }
}
