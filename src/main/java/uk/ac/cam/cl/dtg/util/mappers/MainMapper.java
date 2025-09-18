package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MainMapper extends ContentMapperMS, UserMapper, EventMapper, GameboardMapper {
    MainMapper INSTANCE = Mappers.getMapper(MainMapper.class);
}