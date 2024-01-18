package uk.ac.cam.cl.dtg.segue.etl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.etl.ETLInMemorySshConfigStore.InMemoryHostConfig;

class ETLInMemorySshConfigStoreTest {

  @Test
  void inMemoryHostConfigGetValue_ExistingKey_ReturnsValue() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", Collections.singletonList("no"));

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertEquals("no", hostConfig.getValue("StrictHostKeyChecking"));
  }

  @Test
  void inMemoryHostConfigGetValue_NonExistingKey_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValue("AKeyThatDoesn'tExist"));
  }

  @Test
  void inMemoryHostConfigGetValue_ExistingKeyListValue_ReturnsFirstValueOfList() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", Arrays.asList("no", "host", "checking", "please"));

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertEquals("no", hostConfig.getValue("StrictHostKeyChecking"));
  }

  @Test
  void inMemoryHostConfigGetValue_NullKey_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValue(null));
  }

  @Test
  void inMemoryHostConfigGetValue_ExistingKeyEmptyListValue_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", Collections.emptyList());

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValue("StrictHostKeyChecking"));
  }

  @Test
  void inMemoryHostConfigGetValues_ExistingKey_ReturnsValues() {
    // Arrange
    List<String> options = Arrays.asList("no", "host", "checking", "please");

    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", options);

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertEquals(options, hostConfig.getValues("StrictHostKeyChecking"));
  }

  @Test
  void inMemoryHostConfigGetValues_NonExistingKey_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValues("AKeyThatDoesn'tExist"));
  }

  @Test
  void inMemoryHostConfigGetValues_ExistingKeySingleValue_ReturnsValueAsList() {
    // Arrange
    List<String> option = Collections.singletonList("no");

    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", option);

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertEquals(option, hostConfig.getValues("StrictHostKeyChecking"));
  }

  @Test
  void inMemoryHostConfigGetValues_NullKey_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValues(null));
  }

  @Test
  void inMemoryHostConfigGetValues_ExistingEmptyListValue_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", Collections.emptyList());

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValues(null));
  }

  @Test
  void inMemoryHostConfigGetOptions_MixedSingleAndMultiValue_ReturnsSingleOptionsOnly() {
    // Arrange
    InMemoryHostConfig hostConfig = new InMemoryHostConfig(getMixedTestData());

    // Act & Assert
    Map<String, String> expectedSingleOptions = new HashMap<>();
    expectedSingleOptions.put("LogLevel", "VERBOSE");
    expectedSingleOptions.put("Compression", "yes");

    assertEquals(expectedSingleOptions, hostConfig.getOptions());
  }

  @Test
  void inMemoryHostConfigGetMultiValuedOptions_MixedSingleAndMultiValue_ReturnsMultiValuedOptionsOnly() {
    // Arrange
    InMemoryHostConfig hostConfig = new InMemoryHostConfig(getMixedTestData());

    // Act & Assert
    Map<String, List<String>> expectedMultiValueOptions = new HashMap<>();
    expectedMultiValueOptions.put("Ciphers", Arrays.asList("3des-cbc", "blowfish-cbc", "cast128-cbc"));
    expectedMultiValueOptions.put("HostKeyAlgorithms", Arrays.asList("ssh-rsa", "ssh-dss"));

    assertEquals(expectedMultiValueOptions, hostConfig.getMultiValuedOptions());
  }

  private Map<String, List<String>> getMixedTestData() {
    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("LogLevel", Collections.singletonList("VERBOSE"));
    sshConfig.put("Compression", Collections.singletonList("yes"));
    sshConfig.put("Protocol", Collections.singletonList(null));
    sshConfig.put("ProxyCommand", Collections.emptyList());
    sshConfig.put("Ciphers", Arrays.asList("3des-cbc", "blowfish-cbc", "cast128-cbc"));
    sshConfig.put("HostKeyAlgorithms", Arrays.asList("ssh-rsa", "ssh-dss"));

    return sshConfig;
  }
}
