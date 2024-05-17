package uk.ac.cam.cl.dtg.isaac.mappers;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.isaac.dos.Difficulty;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.RoleRequirement;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;

@Mapper
@SuppressWarnings("unused")
public interface AudienceContextMapper {

  AudienceContextMapper INSTANCE = Mappers.getMapper(AudienceContextMapper.class);

  AudienceContext copy(AudienceContext source);

  Stage copy(Stage source);

  ExamBoard copy(ExamBoard source);

  Difficulty copy(Difficulty source);

  RoleRequirement copy(RoleRequirement source);

  List<AudienceContext> copyListOfAudienceContext(List<AudienceContext> source);

  List<Stage> copyListOfStage(List<Stage> source);

  List<ExamBoard> copyListOfExamBoard(List<ExamBoard> source);

  List<Difficulty> copyListOfDifficulty(List<Difficulty> source);

  List<RoleRequirement> copyListOfRoleRequirement(List<RoleRequirement> source);
}
