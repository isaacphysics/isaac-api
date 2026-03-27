package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.CoordinateItemDTO;

import java.util.List;
import java.util.Objects;

@DTOMapping(CoordinateItemDTO.class)
@JsonContentType("coordinateItem")
public class CoordinateItem extends Item {
    private List<String> coordinates;
    // These two are needed to load old attempts out of the database:
    @Deprecated
    private String x;
    @Deprecated
    private String y;

    /**
     * Default constructor required for mapping.
     */
    public CoordinateItem() {
    }

    /**
     * @param x
     * @param y
     */
    @Deprecated
    public CoordinateItem(final String x, final String y) {
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

    public List<String> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<String> coordinates) {
        this.coordinates = coordinates;
    }

    public CoordinateItem(List<String> coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CoordinateItem)) {
            return false;
        }
        CoordinateItem that = (CoordinateItem) o;
        // We only care about equality of the coordinates, and only for unit testing.
        return Objects.equals(coordinates, that.coordinates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), coordinates);
    }
}
