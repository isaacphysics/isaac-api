package uk.ac.cam.cl.dtg.util.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;

/**
 * MapStruct mapper for EventBooking objects.
 */
@Mapper(uses = UserMapper.class)
public interface EventMapper {

    EventBookingDTO map(DetailedEventBookingDTO source);

    List<EventBookingDTO> copy(List<EventBookingDTO> source);
    List<EventBookingDTO> map(List<DetailedEventBookingDTO> source);

    @Mapping(source = "userBooked", target = "userBooked", qualifiedByName = "copyUserSummaryDTO")
    EventBookingDTO copy(EventBookingDTO source);
}