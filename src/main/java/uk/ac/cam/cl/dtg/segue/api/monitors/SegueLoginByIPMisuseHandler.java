package uk.ac.cam.cl.dtg.segue.api.monitors;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseLogValue;

/**
 * Handler to detect bruteforce login attempts.
 * <p>
 * Preventing users from overusing this endpoint is important as they may be trying to brute force someones password.
 */
public class SegueLoginByIPMisuseHandler implements IMisuseHandler {
    private static final Logger log = LoggerFactory.getLogger(SegueLoginByIPMisuseHandler.class);

    private final Integer softThreshold;
    private final Integer hardThreshold;
    private final Integer accountingInterval;

    @Inject
    public SegueLoginByIPMisuseHandler() {
        this(SEGUE_LOGIN_BY_IP_DEFAULT_SOFT_THRESHOLD, SEGUE_LOGIN_BY_IP_DEFAULT_HARD_THRESHOLD, NUMBER_SECONDS_IN_ONE_HOUR);
    }

    @Inject
    public SegueLoginByIPMisuseHandler(final Integer softThreshold, final Integer hardThreshold, final Integer interval) {
        this.softThreshold = softThreshold;
        this.hardThreshold = hardThreshold;
        this.accountingInterval = interval;
    }

    @Override
    public Integer getSoftThreshold() {
        return softThreshold;
    }

    /*
     * (non-Javadoc)
     *
     * @see uk.ac.cam.cl.dtg.segue.api.managers.IMisuseEvent#getHardThreshold()
     */
    @Override
    public Integer getHardThreshold() {
        return hardThreshold;
    }

    @Override
    public Integer getAccountingIntervalInSeconds() {
        return accountingInterval;
    }

    @Override
    public void executeSoftThresholdAction(final String message) {
        log.warn("Soft threshold limit: " + sanitiseLogValue(message));
    }

    @Override
    public void executeHardThresholdAction(final String message) {
        log.warn("Hard threshold limit: " + sanitiseLogValue(message));
    }
}
