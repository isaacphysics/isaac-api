package uk.ac.cam.cl.dtg.isaac.mappers;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.FormulaValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.ItemValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dto.FormulaValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ItemValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuantityValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;

@Mapper(uses = ContentMapper.class)
public interface QuestionValidationMapper {

  QuestionValidationMapper INSTANCE = Mappers.getMapper(QuestionValidationMapper.class);

  // DO <-> DTO Mappings
  @SubclassMapping(source = FormulaValidationResponseDTO.class, target = FormulaValidationResponse.class)
  @SubclassMapping(source = ItemValidationResponseDTO.class, target = ItemValidationResponse.class)
  @SubclassMapping(source = QuantityValidationResponseDTO.class, target = QuantityValidationResponse.class)
  QuestionValidationResponse map(QuestionValidationResponseDTO source);

  @InheritInverseConfiguration
  QuestionValidationResponseDTO map(QuestionValidationResponse source);
}
