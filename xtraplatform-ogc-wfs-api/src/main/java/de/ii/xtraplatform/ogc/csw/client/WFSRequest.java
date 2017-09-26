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
package de.ii.xtraplatform.ogc.csw.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

/**
 *
 * @author fischer
 */
public class WFSRequest {

    private WFSAdapter wfs;
    private WFSOperation operation;
    private HttpResponse response;
    private ListeningExecutorService pool;

    public WFSRequest(WFSAdapter wfs, WFSOperation operation) {
        this.wfs = wfs;
        this.operation = operation;
        this.pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    }

    public ListenableFuture<HttpEntity> getResponse() {
            return pool.submit(new Callable<HttpEntity>() {
            @Override
            public HttpEntity call() throws Exception {
                return wfs.request(operation);
            }
        });
        
    }

    public String getAsUrl() {
        return wfs.getRequestUrl(operation);
    }
}
