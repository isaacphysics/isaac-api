package uk.ac.cam.cl.dtg.segue.search;

public class RangeInstruction<T> extends AbstractInstruction {
    private String field;
    private long boost = 1;
    private T lessThan;
    private T lessThanOrEqual;
    private T greaterThan;
    private T greaterThanOrEqual;

    public RangeInstruction(String field) {
        this.field = field;
    }

    public String getField() {
        return this.field;
    }

    public long getBoost() {
        return this.boost;
    }
    public RangeInstruction<T> boost(long boost) {
        this.boost = boost;
        return this;
    }

    public T getLessThan() {
        return this.lessThan;
    }
    public RangeInstruction<T> lessThan(T lessThan) {
        this.lessThan = lessThan;
        return this;
    }

    public T getLessThanOrEqual() {
        return this.lessThanOrEqual;
    }
    public RangeInstruction<T> lessThanOrEqual(T lessThanOrEqual) {
        this.lessThanOrEqual = lessThanOrEqual;
        return this;
    }

    public T getGreaterThan() {
        return this.greaterThan;
    }
    public RangeInstruction<T> greaterThan(T greaterThan) {
        this.greaterThan = greaterThan;
        return this;
    }

    public T getGreaterThanOrEqual() {
        return this.greaterThanOrEqual;
    }
    public RangeInstruction<T> greaterThanOrEqual(T greaterThanOrEqual) {
        this.greaterThanOrEqual = greaterThanOrEqual;
        return this;
    }
}
