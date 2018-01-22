package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.api.Constants.FastTrackConceptState;

/**
 * Created by mlt47 on 30/10/2017.
 * TODO MT Document
 */
public class QuestionPartConceptDTO {
    private String title;
    private FastTrackConceptState bestLevel;

    public QuestionPartConceptDTO() {}
    public QuestionPartConceptDTO(String title) {
        this.title = title;
    }
    public final String getTitle() {
        return this.title;
    }
    public final void setTitle(final String title) {
        this.title = title;
    }

    public final FastTrackConceptState getBestLevel() {
        return this.bestLevel;
    }
    public final void setBestLevel(FastTrackConceptState bestLevel) {
        this.bestLevel = bestLevel;
    }

}
