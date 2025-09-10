package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;

@Mapper
public interface ContentMapperMS {
    @SubclassMapping(source = IsaacEventPageDTO.class, target = IsaacEventPageDTO.class)
    ContentDTO copy(ContentDTO source);
}
