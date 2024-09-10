package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.LLMFreeTextMarkSchemeEntryDTO;

@DTOMapping(LLMFreeTextMarkSchemeEntryDTO.class)
public class LLMFreeTextMarkSchemeEntry {
    private String jsonField;
    private String shortDescription;
    private Integer marks;

    public LLMFreeTextMarkSchemeEntry() {
    }

    public String getJsonField() {
        return jsonField;
    }
    public void setJsonField(String jsonField) {
        this.jsonField = jsonField;
    }

    public String getShortDescription() {
        return shortDescription;
    }
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public Integer getMarks() {
        return marks;
    }
    public void setMarks(Integer marks) {
        this.marks = marks;
    }
}
