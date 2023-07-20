package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.api.client.util.Lists;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMetricsExporter;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.Collection;
import java.util.List;

/**
 * SegueContextListener
 * 
 * A class alerts any registered segue listeners as to the servlet context.
 * 
 * @author Stephen Cummins
 *
 */
public class SegueContextNotifier implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(SegueContextNotifier.class);

    public static Injector injector;

    private final IMetricsExporter metricsExporter;
    private final List<ServletContextListener> listeners;

    /**
     * The constructor which will invoke use the Guice injector to get instances of everything that should be notified
     * of any context messages.
     */
    public SegueContextNotifier() {
        injector = SegueGuiceConfigurationModule.getGuiceInjector();

        // Instantiate metrics exporter on process startup
        metricsExporter = injector.getInstance(IMetricsExporter.class);

        listeners = Lists.newArrayList();
        Collection<Class<? extends ServletContextListener>> registeredContextListenerClasses 
            = SegueGuiceConfigurationModule.getRegisteredContextListenerClasses();
        // use guice to initialise everything that needs to be kept informed of
        // the servlet context.
        for (Class<? extends ServletContextListener> segueListener : registeredContextListenerClasses) {
            if (segueListener == SegueContextNotifier.class) {
                // we probably don't want to register ourself.
                continue;
            }

            log.debug("Registering context listener: " + segueListener.toString());
            listeners.add(injector.getInstance(segueListener));
        }
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        log.info("Segue Application Informed of server start up. Registering listeners.");
        for (ServletContextListener listener : listeners) {
            listener.contextInitialized(sce);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        log.info("Segue Application Informed of Shut down - Informing listeners.");
        for (ServletContextListener listener : listeners) {
            listener.contextDestroyed(sce);
        }
    }

}
