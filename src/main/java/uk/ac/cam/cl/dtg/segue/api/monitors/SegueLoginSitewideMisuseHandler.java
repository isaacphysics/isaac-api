package uk.ac.cam.cl.dtg.segue.api.monitors;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

/**
 * Handler to manage suspiciously large numbers of login attempts at a whole-site level.
 */
public class SegueLoginSitewideMisuseHandler implements IMisuseHandler {
    private static final Logger log = LoggerFactory.getLogger(SegueLoginSitewideMisuseHandler.class);

    public static final Integer SOFT_THRESHOLD = 10000;
    public static final Integer HARD_THRESHOLD = 50000;
    public static final Integer ACCOUNTING_INTERVAL = Constants.NUMBER_SECONDS_IN_ONE_DAY;

    private final AbstractConfigLoader properties;
    private final EmailManager emailManager;

    /**
     * Constructor for Guice injection.
     *
     * @param emailManager
     *            - so we can send emails if the threshold limits have been reached.
     * @param properties
     *            - so that we can look up properties set.
     */
    @Inject
    public SegueLoginSitewideMisuseHandler(final EmailManager emailManager, final AbstractConfigLoader properties) {
        this.properties = properties;
        this.emailManager = emailManager;
    }

    @Override
    public Integer getSoftThreshold() {
        return SOFT_THRESHOLD;
    }

    @Override
    public Integer getHardThreshold() {
        return HARD_THRESHOLD;
    }

    @Override
    public Integer getAccountingIntervalInSeconds() {
        return ACCOUNTING_INTERVAL;
    }

    @Override
    public void executeSoftThresholdAction(final String message) {
        final String subject = "Sitewide Soft Threshold limit reached for Segue login attempts";
        EmailCommunicationMessage e = new EmailCommunicationMessage(properties.getProperty(Constants.SERVER_ADMIN_ADDRESS),
                subject, message, message, EmailType.ADMIN);
        emailManager.addSystemEmailToQueue(e);
        log.warn("Soft threshold limit: {}", message);
    }

    @Override
    public void executeHardThresholdAction(final String message) {
        final String subject = "Sitewide HARD Threshold limit reached for Segue login attempts";
        EmailCommunicationMessage e = new EmailCommunicationMessage(properties.getProperty(Constants.SERVER_ADMIN_ADDRESS),
                subject, message, message, EmailType.ADMIN);
        emailManager.addSystemEmailToQueue(e);
        log.error("Hard threshold limit: {}", message);
    }

}
