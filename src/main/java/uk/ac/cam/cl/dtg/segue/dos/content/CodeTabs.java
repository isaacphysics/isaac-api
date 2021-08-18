package uk.ac.cam.cl.dtg.segue.dos.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.ac.cam.cl.dtg.segue.dto.content.CodeTabsDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

@DTOMapping(CodeTabsDTO.class)
@JsonContentType("codeTabs")
public class CodeTabs extends Content {
}
