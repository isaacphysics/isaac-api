package uk.ac.cam.cl.dtg.isaac;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class IsaacE2ETest {
    public static final PostgreSQLContainer postgres;
    public static final ElasticsearchContainer elasticsearch;

    static {
        postgres = new PostgreSQLContainer<>("postgres:12")
                .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
                .withUsername("rutherford")
                .withInitScript("test-postgres-rutherford-create-script.sql")
                .withCommand("postgres", "-c", "fsync=off", "-c", "log_statement=all") // This is for debugging, it may be removed later
        ;

        // TODO It would be nice if we could pull the version from pom.xml
        elasticsearch = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.14.2"))
                .withCopyFileToContainer(MountableFile.forClasspathResource("isaac-test-es-data.tar.gz"), "/usr/share/elasticsearch/isaac-test-es-data.tar.gz")
                .withCopyFileToContainer(MountableFile.forClasspathResource("isaac-test-es-docker-entrypoint.sh"), "/usr/local/bin/docker-entrypoint.sh")
                .withExposedPorts(9200, 9300)
                .withEnv("cluster.name", "isaac")
                .withEnv("node.name", "localhost")
        ;

        postgres.start();
        elasticsearch.start();
    }
}
