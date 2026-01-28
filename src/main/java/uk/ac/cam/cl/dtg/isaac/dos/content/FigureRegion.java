package uk.ac.cam.cl.dtg.isaac.dos.content;

/**
 * Class to store the data needed to represent a region in a figure.
 *
 * Like {@link ExternalReference} this class does not need a DTO mapping,
 * since it does not extend nor involve {@link ContentBase} and the DTO
 * class would otherwise be identical.
 */
public class FigureRegion {
    private String id;
    private String minWidth;
    private Float width;
    private Float left;
    private Float top;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(final String minWidth) {
        this.minWidth = minWidth;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(final Float width) {
        this.width = width;
    }

    public Float getLeft() {
        return left;
    }

    public void setLeft(final Float left) {
        this.left = left;
    }

    public Float getTop() {
        return top;
    }

    public void setTop(final Float top) {
        this.top = top;
    }
}
