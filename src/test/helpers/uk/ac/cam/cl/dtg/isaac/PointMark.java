package uk.ac.cam.cl.dtg.isaac;

import java.util.HashMap;
import java.util.List;

import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;

public class PointMark extends Mark {
    private int pointOne = 0;
    private int pointTwo = 0;
    private int explanationOne = 0;
    private int explanationTwo = 0;

    public PointMark setPointOne(int pointOne) {
        this.pointOne = pointOne;
        return this;
    }

    public PointMark setPointTwo(int pointTwo) {
        this.pointTwo = pointTwo;
        return this;
    }

    public PointMark setExplanationOne(int explanationOne) {
        this.explanationOne = explanationOne;
        return this;
    }

    public PointMark setExplanationTwo(int explanationTwo) {
        this.explanationTwo = explanationTwo;
        return this;
    }

    public HashMap<String, Integer> toHashMap() {
        var result = new HashMap<String, Integer>();
        result.put("pointOne", this.pointOne);
        result.put("pointTwo", this.pointTwo);
        result.put("explanationOne", this.explanationOne);
        result.put("explanationTwo", this.explanationTwo);
        return result;
    }

    public List<LLMFreeTextMarkSchemeEntry> toMarkScheme() {
        return generateMarkScheme(
                Field.field().setName("pointOne").setMarks(pointOne),
                Field.field().setName("pointTwo").setMarks(pointTwo),
                Field.field().setName("explanationOne").setMarks(explanationOne),
                Field.field().setName("explanationTwo").setMarks(explanationTwo));
    }

    public String toJSON() {
        return String.format("{\"pointOne\": %d, \"pointTwo\": %d, \"explanationOne\": %d, \"explanationTwo\": %d}",
                pointOne, pointTwo, explanationOne, explanationTwo);
    }
}