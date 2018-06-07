package uk.ac.cam.cl.dtg.segue.api.monitors;

import com.google.common.primitives.Ints;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * Created by mlt47 on 09/03/2018.
 */
public class PrometheusMetricsExporter implements IMetricsExporter {
    private static final Logger log = LoggerFactory.getLogger(PrometheusMetricsExporter.class);

    /**
     * Constructs a metrics exporter on the port specified for reporting custom logic in a Prometheus style.
     * @param port the port to expose the metrics.
     * @throws IOException could be thrown by the socket.
     */
    public PrometheusMetricsExporter(final int port) throws IOException {
        HTTPServer server = new HTTPServer(port);
    }

    @Override
    public void exposeJvmMetrics() {
        DefaultExports.initialize();
    }
}
