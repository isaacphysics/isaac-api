package uk.ac.cam.cl.dtg.segue.api.metrics;

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

    // Streams Apps Metrics
    public static final Gauge SITE_STATISTICS_STREAMS_APP_STATUS = Gauge.build()
            .name("segue_site_statistics_streams_app_status")
            .help("Site statistics Kafka streams app status (0: down 1: up)").register();
    public static final Gauge USER_STATISTICS_STREAMS_APP_STATUS = Gauge.build()
            .name("segue_user_statistics_streams_app_status")
            .help("User statistics Kafka streams app status (0: down, 1: up)").register();
    public static final Gauge ANONYMOUS_LOGGED_EVENTS_STREAMS_APP_STATUS = Gauge.build()
            .name("segue_anonymous_logged_events_streams_app_status")
            .help("Anonymous logged events Kafka streams app status (0: down, 1: up)").register();

    /**
     *  Private constructor as it does not make sense to instantiate this class.
     */
    private SegueMetrics() { }
}
