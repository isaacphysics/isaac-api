package uk.ac.cam.cl.dtg.segue.api.metrics;

import com.google.common.primitives.Ints;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * Created by mlt47 on 09/03/2018.
 */
public class MetricsExporter {
    private static final Logger log = LoggerFactory.getLogger(MetricsExporter.class);

    /**
     * TODO MT
     */
    public MetricsExporter(final String portString, final boolean exportJvmMetrics) throws IOException {
        log.info("Creating MetricsExporter on port (" + portString + ")");
        int port = Ints.tryParse(portString);
        HTTPServer server = new HTTPServer(port);
        if (exportJvmMetrics) {
            log.info("Exporting default JVM metrics.");
            DefaultExports.initialize();
        }
    }
}
