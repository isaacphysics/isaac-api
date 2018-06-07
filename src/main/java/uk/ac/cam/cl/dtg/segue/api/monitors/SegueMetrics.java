package uk.ac.cam.cl.dtg.segue.api.monitors;

import io.prometheus.client.Gauge;
import io.prometheus.client.guava.cache.CacheMetricsCollector;

/**
 * Created by mlt47 on 09/03/2018.
 * Use this class to register public static final Counters, Gauges and other metrics types used by Segue.
 */
public final class SegueMetrics {
    public static final CacheMetricsCollector CACHE_METRICS_COLLECTOR = new CacheMetricsCollector().register();

    // Websocket Metrics
    public static final Gauge CURRENT_OPEN_WEBSOCKETS = Gauge.build()
            .name("segue_current_open_websockets").help("Currently open websockets.").register();

    // User Metrics
    public static final Gauge CURRENT_WEBSOCKET_USERS = Gauge.build()
            .name("segue_current_websocket_users").help("Currently number of websocket users/browsers.").register();
    // Anonymous user stats are calculated using metrics on the guava cache which holds a reference to each active user

    /**
     *  Private constructor as it does not make sense to instantiate this class.
     */
    private SegueMetrics() { }
}
