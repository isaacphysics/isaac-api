package uk.ac.cam.cl.dtg.segue.dos.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.segue.dto.content.CodeDTO;

/**
 * Code (Abstract) Domain Object To be used anywhere that a figure should be displayed in the CMS.
 *
 */
@DTOMapping(CodeDTO.class)
@JsonContentType("code")
public class Code extends Content {
    protected Content code;
    protected String pythonUrl;

    public Code() {

    }

    public final Content getCode() {
        return code;
    }

    public final void setCode(final Content code) {
        this.code = code;
    }

    public final String getPythonUrl() {
        return this.pythonUrl;
    }

    public final void setPythonUrl(final String pythonUrl) {
        this.pythonUrl = pythonUrl;
    }
}
