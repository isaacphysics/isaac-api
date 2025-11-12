package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.FormulaValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.ItemValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.LLMFreeTextQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dto.FormulaValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ItemValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.LLMFreeTextQuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuantityValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;

/**
 * MapStruct mapper for QuestionValidationResponse objects.
 */
@Mapper(uses = ContentMapper.class)
public interface QuestionValidationMapper {

    QuestionValidationMapper INSTANCE = Mappers.getMapper(QuestionValidationMapper.class);

    @SubclassMapping(source = FormulaValidationResponse.class, target = FormulaValidationResponseDTO.class)
    @SubclassMapping(source = ItemValidationResponse.class, target = ItemValidationResponseDTO.class)
    @SubclassMapping(source = LLMFreeTextQuestionValidationResponse.class, target = LLMFreeTextQuestionValidationResponseDTO.class)
    @SubclassMapping(source = QuantityValidationResponse.class, target = QuantityValidationResponseDTO.class)
    QuestionValidationResponseDTO map(QuestionValidationResponse source);

    @InheritInverseConfiguration
    QuestionValidationResponse map(QuestionValidationResponseDTO source);
}
