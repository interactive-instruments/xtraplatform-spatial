/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.csw.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 *
 * @author zahnen
 */
public class CSWRequest {

    private CSWAdapter csw;
    private CSWOperation operation;
    private HttpResponse response;
    private ListeningExecutorService pool;

    public CSWRequest(CSWAdapter csw, CSWOperation operation) {
        this.csw = csw;
        this.operation = operation;
        this.pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    }

    public ListenableFuture<HttpEntity> getResponse() {
            return pool.submit(new Callable<HttpEntity>() {
            @Override
            public HttpEntity call() throws Exception {
                return csw.request(operation);
            }
        });
        
    }

    public String getAsUrl() {
        return csw.getRequestUrl(operation);
    }
}
