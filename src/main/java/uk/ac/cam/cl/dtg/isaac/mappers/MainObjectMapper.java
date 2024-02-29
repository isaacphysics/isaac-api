package uk.ac.cam.cl.dtg.isaac.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION, uses = {
    AudienceContextMapper.class, ContentMapperSubclasses.class})
public interface MainObjectMapper
    extends ContentMapper, UserMapper, EventMapper, MiscMapper, QuestionValidationMapper {
  MainObjectMapper INSTANCE = Mappers.getMapper(MainObjectMapper.class);
}
