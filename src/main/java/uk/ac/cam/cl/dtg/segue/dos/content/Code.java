package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.CodeDTO;

import java.util.List;

/**
 * Code (Abstract) Domain Object To be used anywhere that a figure should be displayed in the CMS.
 *
 */
@DTOMapping(CodeDTO.class)
@JsonContentType("code")
public class Code extends Content {

    protected List<ContentBase> code;

    public Code() {

    }

    public final List<ContentBase> getCode() {
        return code;
    }

    public final void setCode(final List<ContentBase> code) {
        this.code = code;
    }
}
