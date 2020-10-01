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

import com.google.common.collect.Maps;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.guava.cache.CacheMetricsCollector;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.ISAAC_LOG_TYPES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_LOG_TYPES;

/**
 * Created by mlt47 on 09/03/2018.
 * Use this class to register public static final Counters, Gauges and other metric types used by Segue.
 * Metric and label naming conventions can be found here: https://prometheus.io/docs/practices/naming/
 */
public final class SegueMetrics {
    public static final CacheMetricsCollector CACHE_METRICS_COLLECTOR = new CacheMetricsCollector().register();

    // Websocket Metrics
    public static final Gauge CURRENT_OPEN_WEBSOCKETS = Gauge.build()
            .name("segue_websockets").help("Currently open websockets.").register();
    public static final Counter WEBSOCKETS_OPENED_SUCCESSFULLY = Counter.build()
            .name("segue_websocket_open_total")
            .help("Websockets opened successfully (i.e. not exceeding per user limit) since process start.").register();
    public static final Counter WEBSOCKETS_CLOSED = Counter.build()
            .name("segue_websocket_close_total").help("Websockets closed since process start.").register();

    // User Metrics
    public static final Counter USER_REGISTRATION = Counter.build()
            .name("segue_user_registration_total").help("User registrations since process start.").register();

    public static final Gauge CURRENT_WEBSOCKET_USERS = Gauge.build()
            .name("segue_websocket_users").help("Currently number of websocket users/browsers.").register();
    // Anonymous user stats are calculated using metrics on the guava cache which holds a reference to each active user

    public static final Counter LOG_IN_ATTEMPT = Counter.build()
            .name("segue_log_in_attempt_total").help("Log in attempt since process start.").register();
    public static final Counter LOG_IN = Counter.build()
            .name("segue_log_in_total").help("Successful log in since process start.").register();
    public static final Counter LOG_OUT = Counter.build()
            .name("segue_log_out_total").help("Log out since process start.").register();

    public static final Counter PASSWORD_RESET = Counter.build()
            .name("segue_password_reset_total").help("Password reset requests since process start.").register();

    // Email Metrics
    public static final Counter QUEUED_EMAIL = Counter.build()
            .name("segue_queued_email_total").help("All emails queued since process start").labelNames("type").register();

    // Tracked Log Metrics
    public static final Map<String, Counter> logEventTrackers = Maps.asMap(
            Stream.concat(SEGUE_LOG_TYPES.stream(), ISAAC_LOG_TYPES.stream()).collect(Collectors.toSet()),
            logType -> Counter.build()
                    .name(String.format("segue_log_event_%s", logType.toLowerCase()))
                    .help(String.format("%s log event occurrence counter.", logType)).register());

    /**
     *  Private constructor as it does not make sense to instantiate this class.
     */
    private SegueMetrics() { }
}
