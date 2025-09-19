package uk.ac.cam.cl.dtg.util.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.ac.cam.cl.dtg.isaac.dos.AssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dos.QuizAttemptDO;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;

@Mapper
public interface GameboardMapper {
    GameboardDTO map(GameboardDO source);
    GameboardDO map(GameboardDTO source);

    @Mapping(target = "legacyId", ignore = true)
    @Mapping(target = "groupName", ignore = true)
    @Mapping(target = "gameboard", ignore = true)
    @Mapping(target = "assignerSummary", ignore = true)
    AssignmentDTO map(AssignmentDO source);
    AssignmentDO map(AssignmentDTO source);

    @Mapping(target = "quizSummary", ignore = true)
    @Mapping(target = "quizAssignment", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    @Mapping(target = "feedbackMode", ignore = true)
    QuizAttemptDTO map(QuizAttemptDO source);
    QuizAttemptDO map(QuizAttemptDTO source);
}
