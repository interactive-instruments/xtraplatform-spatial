/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ogc.api.exceptions;

import com.fasterxml.jackson.databind.util.JSONPObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author fischer
 */
public class XtraserverFrameworkException extends WebApplicationException {

    protected String callback;
    protected String msg = "";
    protected String stdmsg = "Error";
    protected Response.Status code = Response.Status.INTERNAL_SERVER_ERROR;
    protected Response.Status htmlCode = Response.Status.INTERNAL_SERVER_ERROR;
    protected List<String> details = new ArrayList();

    public XtraserverFrameworkException(Object m, Object... args) {
        msg = (String) m;//MessageCompiler.compileMessage(m, args);
    }

    public XtraserverFrameworkException() {
    }

    public XtraserverFrameworkException(Throwable cause) {
        super(cause);
    }
    
    @Override
    public String getMessage() {
        return this.getMsg();
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    /*public void setMsg(Object m, Object... args) {
        this.msg = MessageCompiler.compileMessage(m, args);
    }*/

    public void setCallback(String callback) {
        //System.out.println(callback);
        this.callback = callback;
    }

    public String getCallback() {
        return callback;
    }

    public int getHtmlCode() {
        return htmlCode.getStatusCode();
    }

    public void setHtmlCode(Response.Status code) {
        this.htmlCode = code;
    }

    public int getCode() {
        return code.getStatusCode();
    }

    public void setCode(Response.Status code) {
        this.code = code;
    }

    public void addDetail(String detail) {
        this.details.add(detail);
    }
    
    /*public void addDetail(Object m, Object... args) {
        this.details.add(compileMessage(m, args));
    }*/
    
    public List<String> getDetails() {
        return this.details;
    }

    @Override
    public Response getResponse() {

        JsonError error = new JsonError(getCode(), stdmsg);
        error.addDetail(msg);

        for (String detail : this.details) {
            error.addDetail(detail);
        }

        if (callback != null && !callback.isEmpty()) {

            return Response.ok(new JSONPObject(this.callback, error), MediaType.APPLICATION_JSON).build();
        }
        return Response.ok(error, MediaType.APPLICATION_JSON).status(getHtmlCode()).build();
    }
}
