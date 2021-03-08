package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.List;

public class CodeDTO extends ContentDTO {
    protected List<ContentBaseDTO> code;

    public CodeDTO() {

    }

    public List<ContentBaseDTO> getCode() {
        return code;
    }

    public void setCode(final List<ContentBaseDTO> code) {
        this.code = code;
    }
}
