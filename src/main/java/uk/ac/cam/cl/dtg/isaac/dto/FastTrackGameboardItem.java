package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

/**
 * Created by mlt47 on 30/10/2017.
 * TODO MT Document
 */
public class FastTrackGameboardItem extends GameboardItem {
    private List<QuestionPartConceptDTO> questionPartConcepts;

    public FastTrackGameboardItem(GameboardItem gameboardItem) {
        super(gameboardItem);
    }
    public final List<QuestionPartConceptDTO> getQuestionPartConcepts() { return this.questionPartConcepts; }
    public final void setQuestionPartConcepts(List<QuestionPartConceptDTO> questionPartConcepts) {
        this.questionPartConcepts = questionPartConcepts;
    }
}
