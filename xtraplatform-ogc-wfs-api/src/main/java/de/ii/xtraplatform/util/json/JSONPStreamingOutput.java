package de.ii.xtraplatform.util.json;

import de.ii.xsf.core.api.exceptions.XtraserverFrameworkException;
import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

/**
 *
 * @author fischer
 */
public abstract class JSONPStreamingOutput implements StreamingOutput {

    private final String callback;

    public JSONPStreamingOutput(String callback) {
        this.callback = callback;
    }

    @Override
    public void write(OutputStream os) throws IOException, WebApplicationException {
        try {
            this.writeCallback(new JSONPOutputStream(os, callback));
        } catch (XtraserverFrameworkException ex) {
            ex.setCallback(callback);
            ex.setHtmlCode(Response.Status.OK);
            throw ex;
        }

    }

    public abstract void writeCallback(JSONPOutputStream os) throws IOException, WebApplicationException;
}
