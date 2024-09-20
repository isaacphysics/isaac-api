package uk.ac.cam.cl.dtg.isaac.dos.content;

/**
 * Abstract class to represent the fomulae for marking LLM questions.
 */
public abstract class LLMMarkingExpression {
    public abstract String getType();
    public abstract void setType(String type);
}
