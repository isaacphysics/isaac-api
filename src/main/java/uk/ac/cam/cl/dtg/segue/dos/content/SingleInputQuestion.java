package uk.ac.cam.cl.dtg.segue.dos.content;

import java.util.List;

/**
 * Created by ipd21 on 12/06/2017.
 */
public class SingleInputQuestion extends Question {

    // The type of Value to accept as an answer to this question
    protected String inputType;

    // The label to display for the input.
    // TODO: Decide whether this should be a full content object.
    protected String inputLabel;

    // The list of valid choices for this question. They should all be of type inputType, otherwise it won't
    // be possible to submit a valid answer.
    protected List<Choice> choices;



}
