package uk.ac.cam.cl.dtg.util.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.ac.cam.cl.dtg.isaac.dos.AssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardContentDescriptor;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dos.QuizAssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dos.QuizAttemptDO;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;

/**
 * MapStruct mapper for Gameboard, Assignment, QuizAssignment and QuizAttempt objects.
 */
@Mapper
public interface AssignmentMapper {

    @Mapping(target = "ownerUserInformation", ignore = true)
    @Mapping(target = "savedToCurrentUser", ignore = true)
    @Mapping(target = "percentageAttempted", ignore = true)
    @Mapping(target = "percentageCorrect", ignore = true)
    @Mapping(target = "lastVisited", ignore = true)
    @Mapping(target = "startedQuestion", ignore = true)
    GameboardDTO map(GameboardDO source);

    GameboardDO map(GameboardDTO source);

    @Mapping(target = "legacyId", ignore = true)
    @Mapping(target = "gameboard", ignore = true)
    @Mapping(target = "groupName", ignore = true)
    @Mapping(target = "assignerSummary", ignore = true)
    AssignmentDTO map(AssignmentDO source);

    AssignmentDO map(AssignmentDTO source);

    @Mapping(target = "quizSummary", ignore = true)
    @Mapping(target = "assignerSummary", ignore = true)
    @Mapping(target = "attempt", ignore = true)
    @Mapping(target = "userFeedback", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    QuizAssignmentDTO map(QuizAssignmentDO source);

    QuizAssignmentDO map(QuizAssignmentDTO source);

    @Mapping(target = "quizSummary", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    @Mapping(target = "quizAssignment", ignore = true)
    @Mapping(target = "feedbackMode", ignore = true)
    QuizAttemptDTO map(QuizAttemptDO source);

    QuizAttemptDO map(QuizAttemptDTO source);

    // Handle mapping the "content" field for GameboardD(T)Os

    @Mapping(source = "creationContext", target = "context")
    GameboardContentDescriptor mapGameboardItemToGameboardContentDescriptor(GameboardItem source);

    @Mapping(target = "title", ignore = true)
    @Mapping(target = "subtitle", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "level", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "questionPartStates", ignore = true)
    @Mapping(target = "questionPartsCorrect", ignore = true)
    @Mapping(target = "questionPartsIncorrect", ignore = true)
    @Mapping(target = "questionPartsNotAttempted", ignore = true)
    @Mapping(target = "questionPartsTotal", ignore = true)
    @Mapping(target = "passMark", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "boardId", ignore = true)
    @Mapping(target = "supersededBy", ignore = true)
    @Mapping(target = "audience", ignore = true)
    @Mapping(source = "context", target = "creationContext")
    GameboardItem mapGameboardItemToGameboardContentDescriptor(GameboardContentDescriptor source);
}
