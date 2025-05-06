package uk.ac.cam.cl.dtg.isaac;

import java.util.List;

import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;

public class GenericMark extends Mark {
  private int reasonFoo = 0;
  private int reasonBar = 0;
  private int reasonFizz = 0;

  public GenericMark setReasonFoo(int reasonFoo) {
      this.reasonFoo = reasonFoo;
      return this;
  }

  public GenericMark setReasonBar(int reasonBar) {
      this.reasonBar = reasonBar;
      return this;
  }

  public GenericMark setReasonFizz(int reasonFizz) {
      this.reasonFizz = reasonFizz;
      return this;
  }

  public List<LLMFreeTextMarkSchemeEntry> toMarkScheme() {
      return generateMarkScheme(
          Field.field().setName("reasonFoo").setMarks(reasonFoo),
          Field.field().setName("reasonBar").setMarks(reasonBar),
          Field.field().setName("reasonFizz").setMarks(reasonFizz)
      );
  }

  public String toJSON() {
    return String.format("{\"reasonFoo\": %d, \"reasonBar\": %d, \"reasonFizz\": %d}", reasonFoo, reasonBar, reasonFizz); 
  }
}