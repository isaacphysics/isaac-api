package uk.ac.cam.cl.dtg.segue.search;

public class RangeMatchInstruction<T> extends AbstractMatchInstruction {
  private String field;
  private long boost = 1;
  private T lessThan;
  private T lessThanOrEqual;
  private T greaterThan;
  private T greaterThanOrEqual;

  public RangeMatchInstruction(final String field) {
    this.field = field;
  }

  public String getField() {
    return this.field;
  }

  public long getBoost() {
    return this.boost;
  }

  public RangeMatchInstruction boost(final long boost) {
    this.boost = boost;
    return this;
  }

  public T getLessThan() {
    return this.lessThan;
  }

  public RangeMatchInstruction lessThan(final T lessThan) {
    this.lessThan = lessThan;
    return this;
  }

  public T getLessThanOrEqual() {
    return this.lessThanOrEqual;
  }

  public RangeMatchInstruction lessThanOrEqual(final T lessThanOrEqual) {
    this.lessThanOrEqual = lessThanOrEqual;
    return this;
  }

  public T getGreaterThan() {
    return this.greaterThan;
  }

  public RangeMatchInstruction greaterThan(final T greaterThan) {
    this.greaterThan = greaterThan;
    return this;
  }

  public T getGreaterThanOrEqual() {
    return this.greaterThanOrEqual;
  }

  public RangeMatchInstruction greaterThanOrEqual(final T greaterThanOrEqual) {
    this.greaterThanOrEqual = greaterThanOrEqual;
    return this;
  }
}
