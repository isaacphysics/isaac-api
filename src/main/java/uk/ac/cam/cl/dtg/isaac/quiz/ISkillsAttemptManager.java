package uk.ac.cam.cl.dtg.isaac.quiz;

import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateSkillsAttemptException;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/** Persistence interface for recording Anvil skills question attempts. */
public interface ISkillsAttemptManager {
    void registerSkillsAttempt(final AnvilPayloadDTO attempt)
            throws DuplicateSkillsAttemptException, SegueDatabaseException;
}
