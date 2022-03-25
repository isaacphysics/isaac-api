package uk.ac.cam.cl.dtg.segue.dao.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;

import java.io.IOException;

public class ContentMapperCacheLoader extends CacheLoader<String, Content> {

    private static final Logger log = LoggerFactory.getLogger(ContentMapperCacheLoader.class);
    private final ObjectMapper mapper;

    public ContentMapperCacheLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Content load(final String key) throws Exception {
        // todo: remove the following log line
        log.info("Cache miss for deserialized content with ID " + mapper.readTree(key).get("id").toString());
        Content content = null;
        try {
            content = (Content) mapper.readValue(key, ContentBase.class);
        } catch (IOException e) {
            log.error("Error deserializing content JSON with ID " + mapper.readTree(key).get("id").toString(), e);
        }
        return content;
    }
}
