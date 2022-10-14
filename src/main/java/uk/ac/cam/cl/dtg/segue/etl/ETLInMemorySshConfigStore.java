package uk.ac.cam.cl.dtg.segue.etl;

import org.eclipse.jgit.transport.SshConfigStore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * A SshConfigStore used by JGit to configure the SSH sessions it creates.
 * ETL will use the same settings regardless of host and ignore existing on-disk configs.
 */
public class ETLInMemorySshConfigStore implements SshConfigStore {

    private final InMemoryHostConfig inMemoryHostConfig;

    /**
     * @param config - A Map of config keys to config values (of the sort ordinarily found in ~/.ssh/config),
     *               which will apply to all sessions using this store regardless of the target host, port or user.
     */
    public ETLInMemorySshConfigStore(final Map<String, List<String>> config) {
        inMemoryHostConfig = new InMemoryHostConfig(config);
    }

    /**
     * @param hostName - ignored, as we will use the same configuration regardless.
     * @param port - ignored, as we will use the same configuration regardless.
     * @param userName - ignored, as we will use the same configuration regardless.
     * @return A new InMemoryHostConfig holding the provided configuration options.
     */
    @Override
    public HostConfig lookup(final String hostName, final int port, final String userName) {
        return inMemoryHostConfig;
    }

    @Override
    public HostConfig lookupDefault(final String hostName, final int port, final String userName) {
        return inMemoryHostConfig;
    }

    /**
     * A HostConfig that ignores config options on disk, instead using those passed in via the constructor.
     */
    public static class InMemoryHostConfig implements HostConfig {

        private final Map<String, List<String>> config;

        /**
         * @param config - A Map of config keys to config values (of the sort ordinarily found in ~/.ssh/config).
         */
        public InMemoryHostConfig(final Map<String, List<String>> config) {
            this.config = config;
        }

        @Override
        public String getValue(final String key) {
            List<String> value = config.get(key);
            if (null == value || value.size() == 0) {
                return  null;
            }
            return value.get(0);
        }

        @Override
        public List<String> getValues(final String key) {
            return config.get(key);
        }

        @Override
        public Map<String, String> getOptions() {
            return config.entrySet()
                    .stream()
                    .filter(e -> null != e.getValue() && e.getValue().size() == 1 && null != e.getValue().get(0))
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        }

        @Override
        public Map<String, List<String>> getMultiValuedOptions() {
            return config.entrySet()
                    .stream()
                    .filter(e -> null != e.getValue() && e.getValue().size() > 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
}
