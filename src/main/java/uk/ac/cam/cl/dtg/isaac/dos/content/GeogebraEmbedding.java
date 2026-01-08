package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.GeogebraEmbeddingDTO;

@DTOMapping(GeogebraEmbeddingDTO.class)
@JsonContentType("geogebraEmbedding")
public class GeogebraEmbedding extends Media {
    private String appId;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
