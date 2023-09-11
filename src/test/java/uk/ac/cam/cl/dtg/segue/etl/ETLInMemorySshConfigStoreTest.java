package uk.ac.cam.cl.dtg.segue.etl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import uk.ac.cam.cl.dtg.segue.etl.ETLInMemorySshConfigStore.InMemoryHostConfig;

public class ETLInMemorySshConfigStoreTest {

  @Test
  public void inMemoryHostConfigGetValue_ExistingKey_ReturnsValue() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", Collections.singletonList("no"));

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertEquals("no", hostConfig.getValue("StrictHostKeyChecking"));
  }

  @Test
  public void inMemoryHostConfigGetValue_NonExistingKey_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValue("AKeyThatDoesn'tExist"));
  }

  @Test
  public void inMemoryHostConfigGetValue_ExistingKeyListValue_ReturnsFirstValueOfList() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", Arrays.asList("no", "host", "checking", "please"));

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertEquals("no", hostConfig.getValue("StrictHostKeyChecking"));
  }

  @Test
  public void inMemoryHostConfigGetValue_NullKey_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValue(null));
  }

  @Test
  public void inMemoryHostConfigGetValue_ExistingKeyEmptyListValue_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", Collections.emptyList());

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValue("StrictHostKeyChecking"));
  }

  @Test
  public void inMemoryHostConfigGetValues_ExistingKey_ReturnsValues() {
    // Arrange
    List<String> options = Arrays.asList("no", "host", "checking", "please");

    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", options);

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertEquals(options, hostConfig.getValues("StrictHostKeyChecking"));
  }

  @Test
  public void inMemoryHostConfigGetValues_NonExistingKey_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValues("AKeyThatDoesn'tExist"));
  }

  @Test
  public void inMemoryHostConfigGetValues_ExistingKeySingleValue_ReturnsValueAsList() {
    // Arrange
    List<String> option = Collections.singletonList("no");

    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", option);

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertEquals(option, hostConfig.getValues("StrictHostKeyChecking"));
  }

  @Test
  public void inMemoryHostConfigGetValues_NullKey_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValues(null));
  }

  @Test
  public void inMemoryHostConfigGetValues_ExistingEmptyListValue_ReturnsNull() {
    // Arrange
    Map<String, List<String>> sshConfig = new HashMap<>();
    sshConfig.put("StrictHostKeyChecking", Collections.emptyList());

    InMemoryHostConfig hostConfig = new InMemoryHostConfig(sshConfig);

    // Act & Assert
    assertNull(hostConfig.getValues(null));
  }

  @Test
  public void inMemoryHostConfigGetOptions_MixedSingleAndMultiValue_ReturnsSingleOptionsOnly() {
    // Arrange
    InMemoryHostConfig hostConfig = new InMemoryHostConfig(getMixedTestData());

    // Act & Assert
    Map<String, String> expectedSingleOptions = new HashMap<>();
    expectedSingleOptions.put("LogLevel", "VERBOSE");
    expectedSingleOptions.put("Compression", "yes");

    assertEquals(expectedSingleOptions, hostConfig.getOptions());
  }

  @Test
  public void inMemoryHostConfigGetMultiValuedOptions_MixedSingleAndMultiValue_ReturnsMultiValuedOptionsOnly() {
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
