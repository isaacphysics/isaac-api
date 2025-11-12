package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;

import java.util.List;

/**
 * MapStruct mapper for EventBooking objects.
 */
@Mapper(uses = UserMapper.class)
public interface EventBookingMapper {

    EventBookingMapper INSTANCE = Mappers.getMapper(EventBookingMapper.class);

    EventBookingDTO mapToEventBookingDTO(DetailedEventBookingDTO source);

    List<EventBookingDTO> mapToListOfEventBookingDTO(List<DetailedEventBookingDTO> source);

    @Mapping(source = "userBooked", target = "userBooked", qualifiedByName = "copyUserSummaryDTO")
    EventBookingDTO copy(EventBookingDTO source);

    List<EventBookingDTO> copy(List<EventBookingDTO> source);
}