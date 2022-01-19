package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.common.collect.Maps;
import org.quartz.Job;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueScheduledJob;

import java.util.Map;

public class SegueScheduledDeleteEventAdditionalBookingInformationJob extends SegueScheduledJob {

    public SegueScheduledDeleteEventAdditionalBookingInformationJob(String jobKey, String jobGroupName, String description, String cronString) {
        super(jobKey, jobGroupName, description, cronString);
    }

    @Override
    public Map<String, Object> getExecutionContext() {
        return Maps.newHashMap();
    }

    @Override
    public Job getExecutableTask() {
        return new DeleteEventAdditionalBookingInformationJob();
    }
}
