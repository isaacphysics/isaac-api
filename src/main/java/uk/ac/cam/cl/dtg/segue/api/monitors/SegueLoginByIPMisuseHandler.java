package uk.ac.cam.cl.dtg.segue.api.monitors;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_HOUR;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseLogValue;

/**
 * Handler to detect bruteforce login attempts.
 * <p>
 * Preventing users from overusing this endpoint is important as they may be trying to brute force someones password.
 */
public class SegueLoginByIPMisuseHandler implements IMisuseHandler {
    private static final Logger log = LoggerFactory.getLogger(SegueLoginByIPMisuseHandler.class);

    public final Integer SOFT_THRESHOLD;
    public final Integer HARD_THRESHOLD;
    public final Integer ACCOUNTING_INTERVAL;

    @Inject
    public SegueLoginByIPMisuseHandler() {
        this(50, 300, NUMBER_SECONDS_IN_ONE_HOUR);
    }

    @Inject
    public SegueLoginByIPMisuseHandler(Integer softThreshold, Integer hardThreshold, Integer interval) {
        this.SOFT_THRESHOLD = softThreshold;
        this.HARD_THRESHOLD = hardThreshold;
        this.ACCOUNTING_INTERVAL = interval;
    }

    @Override
    public Integer getSoftThreshold() {
        return SOFT_THRESHOLD;
    }

    /*
     * (non-Javadoc)
     *
     * @see uk.ac.cam.cl.dtg.segue.api.managers.IMisuseEvent#getHardThreshold()
     */
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
        log.warn("Soft threshold limit: " + sanitiseLogValue(message));
    }

    @Override
    public void executeHardThresholdAction(final String message) {
        log.warn("Hard threshold limit: " + sanitiseLogValue(message));
    }
}
