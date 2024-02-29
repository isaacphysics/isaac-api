package uk.ac.cam.cl.dtg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomAssertions {
  private static final ObjectMapper jsonMapper = new ObjectMapper();

  public static void assertDeepEquals(Object expected, Object actual) {
    try {
      assertEquals(jsonMapper.writeValueAsString(expected), jsonMapper.writeValueAsString(actual));
    } catch (JsonProcessingException e) {
      fail("Error converting object to json string for comparison", e);
    }
  }
}
