package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.factory.Mappers;

/**
 * Main MapStruct mapper interface.
 */
@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface MainMapper extends ContentMapper, UserMapper, EventMapper, AssignmentMapper, QuestionMapper, QuestionValidationMapper {
    MainMapper INSTANCE = Mappers.getMapper(MainMapper.class);
}