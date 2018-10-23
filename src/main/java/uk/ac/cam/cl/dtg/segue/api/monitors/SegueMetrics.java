/**
 * Copyright 2018 Meurig Thomas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api.monitors;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.guava.cache.CacheMetricsCollector;

/**
 * Created by mlt47 on 09/03/2018.
 * Use this class to register public static final Counters, Gauges and other metrics types used by Segue.
 * Metric and label naming conventions can be found here: https://prometheus.io/docs/practices/naming/
 */
public final class SegueMetrics {
    public static final CacheMetricsCollector CACHE_METRICS_COLLECTOR = new CacheMetricsCollector().register();

    // Websocket Metrics
    public static final Gauge CURRENT_OPEN_WEBSOCKETS = Gauge.build()
            .name("segue_websockets").help("Currently open websockets.").register();
    public static final Counter WEBSOCKETS_OPENED = Counter.build()
            .name("segue_websocket_open_total").help("Websockets opened since process start.").register();
    public static final Counter WEBSOCKETS_CLOSED = Counter.build()
            .name("segue_websocket_close_total").help("Websockets closed since process start.").register();

    // User Metrics
    public static final Gauge CURRENT_WEBSOCKET_USERS = Gauge.build()
            .name("segue_websocket_users").help("Currently number of websocket users/browsers.").register();
    // Anonymous user stats are calculated using metrics on the guava cache which holds a reference to each active user

    /**
     *  Private constructor as it does not make sense to instantiate this class.
     */
    private SegueMetrics() { }
}
