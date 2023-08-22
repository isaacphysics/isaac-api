package uk.ac.cam.cl.dtg.isaac.dos;

public class GameboardContentDescriptor {
    private String id;
    private String contentType;
    private AudienceContext context;

    // Empty constructor necessary fot mappers
    public GameboardContentDescriptor() {
    }

    public GameboardContentDescriptor(final String id, final String contentType, final AudienceContext context) {
        this.id = id;
        this.contentType = contentType;
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public AudienceContext getContext() {
        return context;
    }

    public void setContext(final AudienceContext context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return "GameboardContentDescriptor["
                + "id='" + id + '\''
                + ", contentType='" + contentType + '\''
                + ", context=" + context
                + ']';
    }
}
