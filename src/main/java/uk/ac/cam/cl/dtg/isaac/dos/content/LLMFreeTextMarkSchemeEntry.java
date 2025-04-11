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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LLMFreeTextMarkSchemeEntry)) {
            return false;
        }

        LLMFreeTextMarkSchemeEntry other = (LLMFreeTextMarkSchemeEntry) obj;
        boolean result = true;
        if (this.jsonField != null) {
            result = result && this.jsonField.equals(other.getJsonField());
        }
        if (this.marks != null) {
            result = result && this.marks.equals(other.getMarks());
        }
        if (this.shortDescription != null) {
            result = result && this.shortDescription.equals(other.getShortDescription());
        }
        return result;
    }
}
