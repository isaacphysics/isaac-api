package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacGraphSketcherQuestionDTO;

/**
 * A class instance of choice, where JSON strings of graphs are supposed to be input.
 *
 * Created by hhrl2 on 01/08/2016.
 */
@DTOMapping(IsaacGraphSketcherQuestionDTO.class)
@JsonContentType("graph")
public class Graph extends Choice {

    /**
     * Graph data, in JSON string format.
     */
    private String graphData;

    /**
     * @return the graph data
     */
    public String getGraphData() {
        return graphData;
    }

    /**
     * @param graphData the graph data to set
     */
    public void setGraphData(final String graphData) {
        this.graphData = graphData;
    }
}
