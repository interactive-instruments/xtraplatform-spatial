package de.ii.xtraplatform.ogc.api.wfs.client;

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
}
