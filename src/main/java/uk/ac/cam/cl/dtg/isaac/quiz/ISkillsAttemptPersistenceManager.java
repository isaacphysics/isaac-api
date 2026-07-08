package uk.ac.cam.cl.dtg.isaac.quiz;

import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateSkillsAttemptException;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.util.List;

/** Persistence interface for recording Anvil skills question attempts. */
public interface ISkillsAttemptPersistenceManager {
    void registerSkillsAttempt(final List<AnvilPayloadDTO> attempt)
            throws DuplicateSkillsAttemptException, SegueDatabaseException;
}
