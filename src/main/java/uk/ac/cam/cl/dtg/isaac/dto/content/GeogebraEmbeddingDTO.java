package uk.ac.cam.cl.dtg.isaac.dto.content;

public class GeogebraEmbeddingDTO extends MediaDTO {
    private String materialId;
    private String appType;
    private Boolean allowNewInputs;

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(final String materialId) {
        this.materialId = materialId;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(final String appType) {
        this.appType = appType;
    }

    public Boolean getAllowNewInputs() {
        return allowNewInputs;
    }

    public void setAllowNewInputs(final Boolean allowNewInputs) {
        this.allowNewInputs = allowNewInputs;
    }
}
