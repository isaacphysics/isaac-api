package uk.ac.cam.cl.dtg.segue.dao.content;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;

/**
 * AudienceOrikaConverter A specialist converter class to work with the Orika automapper library.
 * <p> <p>
 * Responsible for converting the intended audience data structure from DO to DTO.
 * This converter will be adopted by Orika whenever it introspects a conversion between these specific types (not only
 * for the audience field). Its implementation is generic so that is fine.
 * It seems ORIKA is not good at converting between highly nested data structures.
 */
public class AudienceOrikaConverter
    extends CustomConverter<List<Map<String, List<String>>>, List<Map<String, List<String>>>> {

  @Override
  public List<Map<String, List<String>>> convert(
      final List<Map<String, List<String>>> maps, final Type<? extends List<Map<String, List<String>>>> type,
      final MappingContext context) {
    if (maps == null) {
      return null;
    }

    // This is horrible to read but it is a deep copy of the data structure - better safe than sorry.
    return maps.stream().map(m -> m.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().collect(Collectors.toList()))))
        .collect(Collectors.toList());
  }
}
