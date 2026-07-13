package uk.ac.cam.cl.dtg.isaac.quiz;

import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateSkillsAttemptException;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Persistence interface for recording Anvil skills question attempts. */
public interface ISkillsAttemptPersistenceManager {
    void registerSkillsAttempt(final List<AnvilPayloadDTO> attempt)
        throws DuplicateSkillsAttemptException, SegueDatabaseException;

    Map<LocalDate, Long> getMentalMathsAttempts(final Long userId, final LocalDate from, final LocalDate to)
        throws SegueDatabaseException;
}
