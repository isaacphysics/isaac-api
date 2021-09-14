package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;

public interface IHasQuizSummary {

    ContentSummaryDTO getQuizSummary();

    void setQuizSummary(ContentSummaryDTO summary);

    String getQuizId();
}
