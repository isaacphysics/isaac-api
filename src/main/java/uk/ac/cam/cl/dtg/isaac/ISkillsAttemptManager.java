package uk.ac.cam.cl.dtg.isaac;

import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;

public interface ISkillsAttemptManager {
    void registerSkillsAttempt(final AnvilPayloadDTO attempt);
}
