/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
