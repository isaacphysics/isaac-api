package uk.ac.cam.cl.dtg.segue.dao.content;

import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class WelshContentManager {
    private final ContentSubclassMapper contentSubclassMapper;

    @Inject
    public WelshContentManager(final ContentSubclassMapper contentSubclassMapper) {
        this.contentSubclassMapper = contentSubclassMapper;
    }

    public final ContentDTO getContentById(final String id, final boolean failQuietly) throws ContentManagerException {
        return this.contentSubclassMapper.getDTOByDO(this.getContentDOById(id, failQuietly));
    }

    public final Content getContentDOById(final String id, final boolean failQuietly) throws ContentManagerException {
        if (null == id || id.isEmpty() || (!id.equals("core_specification_ada") && !id.equals("question_finder_intro") && !id.equals("text_10_v2"))) {
            return null;
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(String.format("src/main/java/uk/ac/cam/cl/dtg/segue/dao/content/%s.cy.json", id))));
            List<Content> searchResults = contentSubclassMapper
                    .mapFromStringListToContentList(List.of(content));

            return searchResults.get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

