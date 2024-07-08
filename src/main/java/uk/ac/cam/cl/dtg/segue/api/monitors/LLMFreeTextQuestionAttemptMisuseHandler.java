package uk.ac.cam.cl.dtg.segue.api.monitors;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

public class LLMFreeTextQuestionAttemptMisuseHandler implements IMisuseHandler {
    private static final Logger log = LoggerFactory.getLogger(QuestionAttemptMisuseHandler.class);

    private static final Integer SOFT_THRESHOLD = 20;
    private static final Integer HARD_THRESHOLD = 30;
    private static final Integer ACCOUNTING_INTERVAL = Constants.NUMBER_SECONDS_IN_ONE_DAY;
    private Integer overrideHardThreshold;

    @Inject
    public LLMFreeTextQuestionAttemptMisuseHandler(final AbstractConfigLoader properties) {
        String overrideThresholdString = properties.getProperty(Constants.LLM_QUESTION_MISUSE_THRESHOLD_OVERRIDE);

        if (null != overrideThresholdString) {
            try {
                this.overrideHardThreshold = Integer.parseInt(overrideThresholdString);
            } catch (NumberFormatException e) {
                log.error("Failed to parse override threshold value: " + overrideThresholdString);
            }
        }
    }

    @Override
    public Integer getSoftThreshold() {
        return SOFT_THRESHOLD;
    }

    @Override
    public Integer getHardThreshold() {
        if (null != overrideHardThreshold) {
            return overrideHardThreshold;
        }
        return HARD_THRESHOLD;
    }

    @Override
    public Integer getAccountingIntervalInSeconds() {
        return ACCOUNTING_INTERVAL;
    }

    @Override
    public void executeSoftThresholdAction(final String message) {
        log.info("Soft threshold limit: " + message);
    }

    @Override
    public void executeHardThresholdAction(final String message) {
        log.warn("Hard threshold limit: " + message);
    }

}
