package uk.ac.cam.cl.dtg.isaac.dto.content;

public class GeogebraEmbeddingDTO extends MediaDTO {
    private String materialId;
    private String appType;
    private Boolean allowNewInputs;

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public Boolean getAllowNewInputs() {
        return allowNewInputs;
    }

    public void setAllowNewInputs(Boolean allowNewInputs) {
        this.allowNewInputs = allowNewInputs;
    }
}
