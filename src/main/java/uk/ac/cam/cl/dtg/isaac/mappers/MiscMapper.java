package uk.ac.cam.cl.dtg.isaac.mappers;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
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
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;

@Mapper
public interface MiscMapper {

  MiscMapper INSTANCE = Mappers.getMapper(MiscMapper.class);

  // DO <-> DTO Mappings
  GameboardDO map(GameboardDTO source);

  @Mapping(target = "startedQuestion", ignore = true)
  @Mapping(target = "savedToCurrentUser", ignore = true)
  @Mapping(target = "percentageCompleted", ignore = true)
  @Mapping(target = "ownerUserInformation", ignore = true)
  @Mapping(target = "lastVisited", ignore = true)
  GameboardDTO map(GameboardDO source);

  AssignmentDO map(AssignmentDTO source);

  @Mapping(target = "legacyId", ignore = true)
  @Mapping(target = "groupName", ignore = true)
  @Mapping(target = "gameboard", ignore = true)
  @Mapping(target = "assignerSummary", ignore = true)
  AssignmentDTO map(AssignmentDO source);

  QuizAssignmentDO map(QuizAssignmentDTO source);

  @Mapping(target = "userFeedback", ignore = true)
  @Mapping(target = "quizSummary", ignore = true)
  @Mapping(target = "quiz", ignore = true)
  @Mapping(target = "attempt", ignore = true)
  @Mapping(target = "assignerSummary", ignore = true)
  QuizAssignmentDTO map(QuizAssignmentDO source);

  QuizAttemptDO map(QuizAttemptDTO source);

  @Mapping(target = "quizSummary", ignore = true)
  @Mapping(target = "quizAssignment", ignore = true)
  @Mapping(target = "quiz", ignore = true)
  @Mapping(target = "feedbackMode", ignore = true)
  QuizAttemptDTO map(QuizAttemptDO source);

  // Mapping an object to a new instance of the same class
  default ResultsWrapper<String> copy(ResultsWrapper<String> source) {
    return new ResultsWrapper<>(copyListOfString(source.getResults()), source.getTotalResults());
  }

  List<String> copyListOfString(List<String> source);

  // Internal object property mappings
  @Mapping(source = "creationContext", target = "context")
  GameboardContentDescriptor mapGameboardItemToGameboardContentDescriptor(GameboardItem source);

  @Mapping(target = "uri", ignore = true)
  @Mapping(target = "title", ignore = true)
  @Mapping(target = "tags", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "state", ignore = true)
  @Mapping(target = "questionPartsTotal", ignore = true)
  @Mapping(target = "questionPartsNotAttempted", ignore = true)
  @Mapping(target = "questionPartsIncorrect", ignore = true)
  @Mapping(target = "questionPartsCorrect", ignore = true)
  @Mapping(target = "questionPartStates", ignore = true)
  @Mapping(target = "passMark", ignore = true)
  @Mapping(target = "level", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "description", ignore = true)
  @Mapping(target = "boardId", ignore = true)
  @Mapping(target = "audience", ignore = true)
  @Mapping(source = "context", target = "creationContext")
  GameboardItem mapGameboardItemToGameboardContentDescriptor(GameboardContentDescriptor source);
}
