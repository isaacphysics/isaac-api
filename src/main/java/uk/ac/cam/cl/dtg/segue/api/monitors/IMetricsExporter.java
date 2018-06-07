package uk.ac.cam.cl.dtg.segue.api.monitors;

/**
 * Created by mlt47 on 06/06/2018.
 */
public interface IMetricsExporter {
    /**
     * Set-up the metrics exporter to report its JVM metrics.
     */
    void exposeJvmMetrics();
}
