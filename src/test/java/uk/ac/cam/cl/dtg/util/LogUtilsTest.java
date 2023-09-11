package uk.ac.cam.cl.dtg.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseLogValue;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class LogUtilsTest {

  @ParameterizedTest
  @MethodSource("valuesToTest")
  public void testSanitiseLogValue(String initialValue, String expectedSanitisedValue) {
    assertEquals(expectedSanitisedValue, sanitiseLogValue(initialValue));
  }

  private static Stream<Arguments> valuesToTest() {
    return Stream.of(
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
}
