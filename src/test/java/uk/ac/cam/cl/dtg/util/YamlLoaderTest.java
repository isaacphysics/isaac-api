package uk.ac.cam.cl.dtg.util;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        YamlLoader loader = getTestableYamlLoader("/some/external/file/path.properties", false);
        assertEquals("src/test/resources/content_indices.test.properties", loader.getProperty("CONTENT_INDICES_LOCATION"));
    }

    @Test
    public void yamlLoader_usingMultipleConfigFiles_firstFileValuesAreOverriddenBySecond() throws Exception {
        YamlLoader loader = getTestableYamlLoader("/some/external/file/path.properties,"
                        + "/another/external/file/path.properties", true);
        assertEquals("somewhere/on/my/machine.properties", loader.getProperty("CONTENT_INDICES_LOCATION"));
    }

    private YamlLoader getTestableYamlLoader(final String paths, final boolean includeOverride) throws Exception {
        // Mock on-disk config files
        // main config file
        Map<String, String> mockConfig = new ConcurrentHashMap<>();
        mockConfig.put("CONTENT_INDICES_LOCATION", "src/test/resources/content_indices.test.properties");

        FileInputStream mockConfigFileInputStream = EasyMock.createNiceMock(FileInputStream.class);

        // override config file
        Map<String, String> mockOverrideConfig = new ConcurrentHashMap<>();
        mockOverrideConfig.put("CONTENT_INDICES_LOCATION", "somewhere/on/my/machine.properties");

        FileInputStream mockOverrideFileInputStream = EasyMock.createNiceMock(FileInputStream.class);

        Yaml mockYaml = EasyMock.createMock(Yaml.class);

        // configure mock YAML parser to return mock configs
        if (includeOverride) {
            EasyMock.expect(mockYaml.load(EasyMock.anyObject(InputStream.class)))
                    .andReturn(mockConfig)
                    .andReturn(mockOverrideConfig);
        } else {
            EasyMock.expect(mockYaml.load(EasyMock.anyObject(InputStream.class)))
                    .andReturn(mockConfig);
        }

        EasyMock.replay(mockConfigFileInputStream, mockOverrideFileInputStream, mockYaml);

        // This cannot be a (partial) mock because we need to set configPath
        return new YamlLoader(paths) {
            @Override
            protected Yaml createYaml() {
                return mockYaml;
            }

            @Override
            protected FileInputStream createFileInputStream(final String path) {
                return includeOverride ? mockOverrideFileInputStream : mockConfigFileInputStream;
            }
        };
    }
}