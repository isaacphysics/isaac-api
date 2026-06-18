package uk.ac.cam.cl.dtg.isaac;

import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;

import java.sql.SQLException;

public interface ISkillsAttemptManager {
    void registerSkillsAttempt(final AnvilPayloadDTO attempt) throws SQLException;
}
