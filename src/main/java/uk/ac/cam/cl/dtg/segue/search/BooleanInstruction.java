package uk.ac.cam.cl.dtg.segue.search;

import com.google.api.client.util.Lists;

import java.util.List;

public class BooleanInstruction extends AbstractInstruction {
    private List<AbstractInstruction> musts = Lists.newArrayList();
    private List<AbstractInstruction> shoulds = Lists.newArrayList();
    private List<AbstractInstruction> mustNots = Lists.newArrayList();
    private int minimumShouldMatch = 0;
    private Float boost;

    public BooleanInstruction() {
        super();
    }

    public BooleanInstruction(int minimumShouldMatch) {
        this.setMinimumShouldMatch(minimumShouldMatch);
    }

    public List<AbstractInstruction> getMusts() {
        return musts;
    }
    public void must(final AbstractInstruction abstractInstruction) {
        musts.add(abstractInstruction);
    }

    public List<AbstractInstruction> getShoulds() {
        return shoulds;
    }
    public void should(final AbstractInstruction abstractInstruction) {
        shoulds.add(abstractInstruction);
    }

    public List<AbstractInstruction> getMustNots() {
        return mustNots;
    }
    public void mustNot(final AbstractInstruction abstractInstruction) {
        mustNots.add(abstractInstruction);
    }

    public int getMinimumShouldMatch() {
        return minimumShouldMatch;
    }
    public void setMinimumShouldMatch(int minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
    }

    public void setBoost(final Float boostValue) {
        this.boost = boostValue;
    }
    public Float getBoost() {
        return this.boost;
    }
}
