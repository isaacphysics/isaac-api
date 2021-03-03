package uk.ac.cam.cl.dtg.segue.dto.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CodeDTO extends ContentDTO {
    protected ContentDTO code;
    protected String pythonUrl;

    @JsonCreator
    public CodeDTO(@JsonProperty("code") ContentDTO code, @JsonProperty("pythonUrl") String pythonUrl) {
        this.code = code;
        this.pythonUrl = pythonUrl;
    }

    public ContentDTO getCode() {
        return this.code;
    }

    public void setCode(ContentDTO code) {
        this.code = code;
    }

    public String getPythonUrl() {
        return this.pythonUrl;
    }

    public void setPythonUrl(String pythonUrl) {
        this.pythonUrl = pythonUrl;
    }
}
