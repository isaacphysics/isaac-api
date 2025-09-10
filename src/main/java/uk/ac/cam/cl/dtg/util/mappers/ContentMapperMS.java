package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassMapping;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;

@Mapper
public interface ContentMapperMS {
    @SubclassMapping(source = IsaacEventPageDTO.class, target = IsaacEventPageDTO.class)
    ContentDTO copy(ContentDTO source);

    default <T> T map(ContentDTO source, Class<T> targetClass) {
        if (targetClass.equals(ContentSummaryDTO.class)) {
            return (T) mapContentDTOtoContentSummaryDTO(source);
        } else {
            throw new RuntimeException();
        }
    }

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "supersededBy", ignore = true)
    @Mapping(target = "summary", ignore = true)
    @Mapping(target = "questionPartIds", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    ContentSummaryDTO mapContentDTOtoContentSummaryDTO(ContentDTO source);
}
