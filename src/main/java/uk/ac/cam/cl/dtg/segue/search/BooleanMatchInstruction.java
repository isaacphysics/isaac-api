package uk.ac.cam.cl.dtg.segue.search;

import com.google.api.client.util.Lists;

import java.util.List;

public class BooleanMatchInstruction extends AbstractMatchInstruction {
    private List<AbstractMatchInstruction> musts = Lists.newArrayList();
    private List<AbstractMatchInstruction> shoulds = Lists.newArrayList();
    private List<AbstractMatchInstruction> mustNots = Lists.newArrayList();
    private int minimumShouldMatch = 0;

    public List<AbstractMatchInstruction> getMusts() {
        return musts;
    }
    public void must(final AbstractMatchInstruction abstractMatchInstruction) {
        musts.add(abstractMatchInstruction);
    }

    public List<AbstractMatchInstruction> getShoulds() {
        return shoulds;
    }
    public void should(final AbstractMatchInstruction abstractMatchInstruction) {
        shoulds.add(abstractMatchInstruction);
    }

    public List<AbstractMatchInstruction> getMustNots() {
        return mustNots;
    }
    public void mustNot(final AbstractMatchInstruction abstractMatchInstruction) {
        mustNots.add(abstractMatchInstruction);
    }

    public int getMinimumShouldMatch() {
        return minimumShouldMatch;
    }
    public void setMinimumShouldMatch(int minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
    }
}
