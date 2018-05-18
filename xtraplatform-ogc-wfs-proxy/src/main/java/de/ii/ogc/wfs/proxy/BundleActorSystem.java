package de.ii.ogc.wfs.proxy;

import akka.actor.ActorSystem;
import akka.osgi.OsgiActorSystemFactory;
import akka.util.Timeout;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.*;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.osgi.framework.BundleContext;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeoutException;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {BundleActorSystem.class})
@Instantiate
public class BundleActorSystem /*extends ActorSystemActivator*/ {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(BundleActorSystem.class);

    @Context
    private BundleContext context;

    @Controller
    private boolean started;

    private ActorSystem system;

    public ActorSystem getSystem() {
        return system;
    }

    @Validate
    private void onStart() {
        LOGGER.getLogger().debug("AKKA STARTING");
        try {
            final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
                    .put("akka.loglevel", "INFO")
                    //.put("akka.log-config-on-start", true)
                    .put("akka.http.host-connection-pool.max-connections", 32)
                    .put("akka.http.host-connection-pool.pool-implementation", "new")
                    .put("akka.http.parsing.max-chunk-size", "16m")
                    .build());
            this.system = new OsgiActorSystemFactory(context, scala.Option.empty(), ConfigFactory.load(config)).createActorSystem(scala.Option.empty());
            this.started = true;
            LOGGER.getLogger().debug("AKKA STARTED");
        } catch (Throwable e) {
            LOGGER.getLogger().debug("AKKA START FAILED", e);
        }
    }

    @Invalidate
    private void onStop() {
        LOGGER.getLogger().debug("AKKA STOPPING");
        Timeout timeout = new Timeout(Duration.create(5, "seconds"));
        try {
            Await.ready(system.terminate(), timeout.duration());
            LOGGER.getLogger().debug("AKKA STOPPED");
        } catch (InterruptedException | TimeoutException e) {
            LOGGER.getLogger().debug("AKKA STOP TIMEOUT");
        } catch (Throwable e) {
            LOGGER.getLogger().debug("AKKA STOP FAILED", e);
        }
    }
    /*@Override
    public void configure(BundleContext context, ActorSystem system) {
        LOGGER.getLogger().debug("AKKA STARTED: {}", system.name());
        new OsgiActorSystemFactory(context, scala.Option.empty() ,ConfigFactory.empty()).createActorSystem(scala.Option.empty());
        // optionally register the ActorSystem in the OSGi Service Registry
        //registerService(context, system)
    }*/
}
