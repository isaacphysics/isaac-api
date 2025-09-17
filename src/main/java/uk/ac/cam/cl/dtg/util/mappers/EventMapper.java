package uk.ac.cam.cl.dtg.util.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;

@Mapper(uses = UserMapper.class)
public interface EventMapper {
    default <T extends EventBookingDTO> T map(DetailedEventBookingDTO source, Class<T> targetClass) {
        if (targetClass.equals(EventBookingDTO.class)) {
            return (T) mapDetailedEventBookingDTOtoEventBookingDTO(source);
        } else {
            throw new UnimplementedMappingException(DetailedEventBookingDTO.class, targetClass);
        }
    }

    default <S extends EventBookingDTO, T extends EventBookingDTO> List<T> mapAsList(List<S> source, Class<S> sourceClass, Class<T> targetClass) {
        if (sourceClass.equals(EventBookingDTO.class) && targetClass.equals(EventBookingDTO.class)) {
            return (List<T>) copyListOfEventBookingDTO((List<EventBookingDTO>) source);
        } else if (sourceClass.equals(DetailedEventBookingDTO.class) && targetClass.equals(EventBookingDTO.class)) {
            return (List<T>) mapListOfDetailedEventBookingDTOtoEventBookingDTO((List<DetailedEventBookingDTO>) source);
        } else {
            throw new UnimplementedMappingException(sourceClass, targetClass);
        }
    }

    @Mapping(source = "userBooked", target = "userBooked", qualifiedByName = "copyUserSummaryDTO")
    EventBookingDTO copy(EventBookingDTO source);
    List<EventBookingDTO> copyListOfEventBookingDTO(List<EventBookingDTO> source);
    EventBookingDTO mapDetailedEventBookingDTOtoEventBookingDTO(DetailedEventBookingDTO source);
    List<EventBookingDTO> mapListOfDetailedEventBookingDTOtoEventBookingDTO(List<DetailedEventBookingDTO> source);
}