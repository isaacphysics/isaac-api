package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.segue.dos.AudienceContext;

public class GameboardContentDescriptor {
    private String id;
    private String contentType;
    private AudienceContext context;

    public GameboardContentDescriptor() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public AudienceContext getContext() {
        return context;
    }

    public void setContext(AudienceContext context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return "GameboardContentItem [" +
                "id='" + id + '\'' +
                ", contentType='" + contentType + '\'' +
                ", context=" + context +
                ']';
    }
}
