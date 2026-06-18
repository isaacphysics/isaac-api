package uk.ac.cam.cl.dtg.isaac.quiz;

import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;

import java.sql.SQLException;

/** Persistence interface for recording Anvil skills question attempts. */
public interface ISkillsAttemptManager {
    void registerSkillsAttempt(final AnvilPayloadDTO attempt) throws SQLException;
}
