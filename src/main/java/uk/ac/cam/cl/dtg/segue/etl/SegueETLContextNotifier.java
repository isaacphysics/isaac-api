package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * SegueContextListener
 *
 * A class alerts any registered segue listeners as to the servlet context.
 *
 * @author Stephen Cummins
 *
 */
public class SegueETLContextNotifier implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(SegueETLContextNotifier.class);

    /**
     * The constructor which will invoke use the Guice injector to get instances of everything that should be notified
     * of any context messages.
     */
    public SegueETLContextNotifier() {

    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        log.info("Segue Application Informed of server start up. Registering listeners.");
        Injector injector = ETLConfigurationModule.getGuiceInjector();

        // Make sure the ETLManager has been created, forcing latest content to be indexed.
        injector.getInstance(ETLManager.class);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        log.info("Segue Application Informed of Shut down - Informing listeners.");
    }

}
