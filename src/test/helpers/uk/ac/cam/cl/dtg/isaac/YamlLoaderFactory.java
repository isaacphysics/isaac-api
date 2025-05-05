package uk.ac.cam.cl.dtg.isaac;

import java.io.IOException;

import uk.ac.cam.cl.dtg.util.YamlLoader;

public class YamlLoaderFactory {
      public static YamlLoader propertiesForTest() throws IOException {
        return new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml,"
                        + "src/test/resources/segue-unit-test-llm-validator-override.yaml");
    }
}
