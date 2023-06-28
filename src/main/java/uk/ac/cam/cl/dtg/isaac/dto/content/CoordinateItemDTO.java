package uk.ac.cam.cl.dtg.isaac.dto.content;

public class CoordinateItemDTO extends ItemDTO {
    private String x;
    private String y;

    /**
     * Default constructor required for mapping.
     */
    public CoordinateItemDTO() {
    }

    /**
     * @param x
     * @param y
     */
    public CoordinateItemDTO(final String x, final String y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return the x
     */
    public String getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(final String x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public String getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(final String y) {
        this.y = y;
    }
}
