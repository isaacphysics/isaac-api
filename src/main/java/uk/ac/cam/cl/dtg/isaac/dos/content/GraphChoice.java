package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.GraphChoiceDTO;

/**
 * A class instance of choice, where JSON strings of graphs are supposed to be input.
 *
 * Created by hhrl2 on 01/08/2016.
 */
@DTOMapping(GraphChoiceDTO.class)
@JsonContentType("graphChoice")
public class GraphChoice extends Choice {

    /**
     * GraphChoice data, in JSON string format.
     */
    private String graphSpec;

    /**
     * @return the graph data
     */
    public String getGraphSpec() {
        return graphSpec;
    }

    /**
     * @param graphSpec the graph data to set
     */
    public void setGraphSpec(final String graphSpec) {
        this.graphSpec = graphSpec;
    }
}
