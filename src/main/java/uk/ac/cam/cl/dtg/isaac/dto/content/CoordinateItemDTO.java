package uk.ac.cam.cl.dtg.isaac.dto.content;

import java.util.List;

public class CoordinateItemDTO extends ItemDTO {
    private List<String> coordinates;
    // These two are needed to load old attempts out of the database:
    @Deprecated
    private String x;
    @Deprecated
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
    @Deprecated
    public CoordinateItemDTO(final String x, final String y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return the x
     */
    @Deprecated
    public String getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    @Deprecated
    public void setX(final String x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    @Deprecated
    public String getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    @Deprecated
    public void setY(final String y) {
        this.y = y;
    }

    public CoordinateItemDTO(List<String> coordinates) {
        this.coordinates = coordinates;
    }

    public List<String> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<String> coordinates) {
        this.coordinates = coordinates;
    }
}
