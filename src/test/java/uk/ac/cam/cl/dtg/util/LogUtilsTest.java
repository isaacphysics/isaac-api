package uk.ac.cam.cl.dtg.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseInternalLogValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class LogUtilsTest {

  @ParameterizedTest
  @MethodSource("stringValuesToTest")
  public void testSanitiseInternalLogValue(String initialValue, String expectedSanitisedValue) {
    assertEquals(expectedSanitisedValue, sanitiseInternalLogValue(initialValue));
  }

  @ParameterizedTest
  @MethodSource("stringValuesToTest")
  public void testSanitiseUserLogValue(String initialValue, String expectedSanitisedValue) {
    assertEquals(expectedSanitisedValue, sanitiseExternalLogValue(initialValue));
  }

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  private static Stream<Arguments> stringValuesToTest() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of("", ""),
        Arguments.of(" ", " "),
        Arguments.of("test", "test"),
        Arguments.of("1234", "1234"),
        Arguments.of("\n", "\\n"),
        Arguments.of("!\"#$%&'()*+-./:;<=>?@[\\]^_`{|}~", "!\\\"#$%&'()*+-./:;<=>?@[\\\\]^_`{|}~"),
        Arguments.of("\u000E\u0015\u001C\u008D", "\\u000E\\u0015\\u001C\\u008D"),
        Arguments.of("\u00A0\u00A7\u00C7\u00EB", "\\u00A0\\u00A7\\u00C7\\u00EB")
    );
  }

  @ParameterizedTest
  @MethodSource("mapValuesToTest")
  public void testSanitiseUserLogValueMap(Map<String, Object> initialValue, String expectedSanitisedValue) {
    assertEquals(expectedSanitisedValue, sanitiseExternalLogValue(initialValue));
  }

  private static Stream<Arguments> mapValuesToTest() {
    // Multi-key map must have consistent ordering to ensure consistency for testing purposes
    Map<String, Object> orderedMap = new LinkedHashMap<>();
    orderedMap.put("key1", "value1");
    orderedMap.put("key2", "value2");
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(Map.of(), "{}"),
        Arguments.of(Map.of("", ""), "{=}"),
        Arguments.of(Map.of("key1", ""), "{key1=}"),
        Arguments.of(Map.of("", "value1"), "{=value1}"),
        Arguments.of(Map.of("key1", "value1"), "{key1=value1}"),
        Arguments.of(orderedMap, "{key1=value1, key2=value2}"),
        Arguments.of(Map.of("key1", Map.of("key2", "value2")), "{key1={key2=value2}}"),
        Arguments.of(Map.of("key1", List.of("value1", "value2")), "{key1=[value1, value2]}"),
        Arguments.of(Map.of("key1", "value1\nvalue2"), "{key1=value1\\nvalue2}")
    );
  }
}
