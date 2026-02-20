package uk.ac.cam.cl.dtg.util;

import org.easymock.EasyMock;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.TestCase.assertEquals;

public class YamlLoaderTest {
    @Test
    public void yamlLoader_usingSingleResourceConfigFile_loadsSuccessfully() throws IOException {
        // Act
        YamlLoader loader = new YamlLoader("src/test/resources/segue-integration-test-config.yaml");

        // Assert
        assertEquals("src/test/resources/content_indices.test.properties", loader.getProperty("CONTENT_INDICES_LOCATION"));
    }

    @Test
    public void yamlLoader_usingMultipleResourceConfigFiles_firstFileValuesAreOverriddenBySecond() throws IOException {
        // Act
        YamlLoader loader = new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml,src/test/resources/segue-override-test-config.yaml"
        );

        // Assert
        assertEquals("somewhere/on/my/machine.properties", loader.getProperty("CONTENT_INDICES_LOCATION"));
    }

    @Test
    public void yamlLoader_usingSingleConfigFile_loadsSuccessfully() throws Exception {
        // Arrange
        setUpMockConfigs();

        // Act
        YamlLoader loader = new YamlLoader("/some/external/file/path.properties");

        // Assert
        assertEquals("src/test/resources/content_indices.test.properties", loader.getProperty("CONTENT_INDICES_LOCATION"));
    }

    @Test
    public void yamlLoader_usingMultipleConfigFiles_firstFileValuesAreOverriddenBySecond() throws Exception {
        // Arrange
        setUpMockConfigs();

        // Act
        YamlLoader loader = new YamlLoader(
                "/some/external/file/path.properties,/another/external/file/path.properties"
        );

        // Assert
        assertEquals("somewhere/on/my/machine.properties", loader.getProperty("CONTENT_INDICES_LOCATION"));
    }

    public void setUpMockConfigs() throws Exception {
        // Mock on-disk config files
        // main config file
        String configPath = "/some/external/file/path.properties";

        Map<String, String> mockConfig = new ConcurrentHashMap<>();
        mockConfig.put("CONTENT_INDICES_LOCATION", "src/test/resources/content_indices.test.properties");

        FileInputStream mockConfigFileInputStream = EasyMock.createNiceMock(FileInputStream.class);
        EasyMock.replay(mockConfigFileInputStream);

        PowerMock.expectNew(FileInputStream.class, configPath).andReturn(mockConfigFileInputStream);

        // override config file
        String overridePath = "/another/external/file/path.properties";

        Map<String, String> mockOverrideConfig = new ConcurrentHashMap<>();
        mockOverrideConfig.put("CONTENT_INDICES_LOCATION", "somewhere/on/my/machine.properties");

        FileInputStream mockOverrideFileInputStream = EasyMock.createNiceMock(FileInputStream.class);
        EasyMock.replay(mockOverrideFileInputStream);

        PowerMock.expectNew(FileInputStream.class, overridePath).andReturn(mockOverrideFileInputStream);

        // configure mock YAML parser to return mock configs
        PowerMock.replay(FileInputStream.class);

        Yaml mockYaml = EasyMock.createMock(Yaml.class);
        EasyMock.expect(mockYaml.load(mockConfigFileInputStream)).andStubReturn(mockConfig);
        EasyMock.expect(mockYaml.load(mockOverrideFileInputStream)).andStubReturn(mockOverrideConfig);
        EasyMock.replay(mockYaml);
        PowerMock.expectNew(Yaml.class).andReturn(mockYaml);
        PowerMock.replay(Yaml.class);
    }
}
