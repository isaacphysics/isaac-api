package uk.ac.cam.cl.dtg.isaac.dos.content;

/**
 * Abstract class to represent the fomulae for marking LLM questions.
 */
public abstract class LLMMarkingExpression {
    abstract String getType();
    abstract void setType(String type);
}
