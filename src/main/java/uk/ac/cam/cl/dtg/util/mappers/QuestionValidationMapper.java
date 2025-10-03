package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;
import uk.ac.cam.cl.dtg.isaac.dos.FormulaValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.ItemValidationResponse;
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

    @SubclassMapping(source = FormulaValidationResponseDTO.class, target = FormulaValidationResponse.class)
    @SubclassMapping(source = ItemValidationResponseDTO.class, target = ItemValidationResponse.class)
    @SubclassMapping(source = LLMFreeTextQuestionValidationResponseDTO.class, target = QuestionValidationResponse.class)
    @SubclassMapping(source = QuantityValidationResponseDTO.class, target = QuantityValidationResponse.class)
    QuestionValidationResponse map(QuestionValidationResponseDTO source);

    @InheritInverseConfiguration
    QuestionValidationResponseDTO map(QuestionValidationResponse source);
}
