package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacAnvilQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacClozeQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCoordinateQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacGraphSketcherQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacItemQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacMultiChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacParsonsQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacRegexMatchQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacReorderQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacStringMatchQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicChemistryQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicLogicQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacAnvilQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacClozeQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCoordinateQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFreeTextQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacGraphSketcherQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacItemQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacLLMFreeTextQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacMultiChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacNumericQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacParsonsQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuickQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacRegexMatchQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacReorderQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacStringMatchQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicChemistryQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicLogicQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;

/**
 * MapStruct mapper for Questions.
 */
@Mapper(uses = ContentMapper.class, subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface QuestionMapper {
    QuestionMapper INSTANCE = Mappers.getMapper(QuestionMapper.class);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = ChoiceQuestion.class, target = ChoiceQuestionDTO.class)
    QuestionDTO map(Question source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacQuestionBase.class, target = IsaacQuestionBaseDTO.class)
    ChoiceQuestionDTO map(ChoiceQuestion source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacQuickQuestion.class, target = IsaacQuickQuestionDTO.class)
    @SubclassMapping(source = IsaacClozeQuestion.class, target = IsaacClozeQuestionDTO.class)
    @SubclassMapping(source = IsaacFreeTextQuestion.class, target = IsaacFreeTextQuestionDTO.class)
    @SubclassMapping(source = IsaacGraphSketcherQuestion.class, target = IsaacGraphSketcherQuestionDTO.class)
    @SubclassMapping(source = IsaacItemQuestion.class, target = IsaacItemQuestionDTO.class)
    @SubclassMapping(source = IsaacMultiChoiceQuestion.class, target = IsaacMultiChoiceQuestionDTO.class)
    @SubclassMapping(source = IsaacNumericQuestion.class, target = IsaacNumericQuestionDTO.class)
    @SubclassMapping(source = IsaacParsonsQuestion.class, target = IsaacParsonsQuestionDTO.class)
    @SubclassMapping(source = IsaacRegexMatchQuestion.class, target = IsaacRegexMatchQuestionDTO.class)
    @SubclassMapping(source = IsaacReorderQuestion.class, target = IsaacReorderQuestionDTO.class)
    @SubclassMapping(source = IsaacStringMatchQuestion.class, target = IsaacStringMatchQuestionDTO.class)
    @SubclassMapping(source = IsaacSymbolicChemistryQuestion.class, target = IsaacSymbolicChemistryQuestionDTO.class)
    @SubclassMapping(source = IsaacSymbolicLogicQuestion.class, target = IsaacSymbolicLogicQuestionDTO.class)
    @SubclassMapping(source = IsaacSymbolicQuestion.class, target = IsaacSymbolicQuestionDTO.class)
    @SubclassMapping(source = IsaacCoordinateQuestion.class, target = IsaacCoordinateQuestionDTO.class)
    @SubclassMapping(source = IsaacLLMFreeTextQuestion.class, target = IsaacLLMFreeTextQuestionDTO.class)
    @SubclassMapping(source = IsaacAnvilQuestion.class, target = IsaacAnvilQuestionDTO.class)
    IsaacQuestionBaseDTO mapIsaacQuestionBase(IsaacQuestionBase source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacClozeQuestion.class, target = IsaacClozeQuestionDTO.class)
    @SubclassMapping(source = IsaacParsonsQuestion.class, target = IsaacParsonsQuestionDTO.class)
    @SubclassMapping(source = IsaacReorderQuestion.class, target = IsaacReorderQuestionDTO.class)
    IsaacItemQuestionDTO mapIsaacItemQuestion(IsaacItemQuestion source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacSymbolicLogicQuestion.class, target = IsaacSymbolicLogicQuestionDTO.class)
    IsaacSymbolicQuestionDTO mapIsaacSymbolicQuestion(IsaacSymbolicQuestion source);
}
