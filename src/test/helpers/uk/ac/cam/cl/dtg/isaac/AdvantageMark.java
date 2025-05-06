package uk.ac.cam.cl.dtg.isaac;

import java.util.HashMap;
import java.util.List;

import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;

public class AdvantageMark extends Mark {
  private int advantageOne = 0;
  private int advantageTwo = 0;
  private int disadvantageOne = 0;
  private int disadvantageTwo = 0;

  public AdvantageMark setAdvantageOne(int advantageOne) {
    this.advantageOne = advantageOne;
    return this;
  }

  public AdvantageMark setAdvantageTwo(int advantageTwo) {
    this.advantageTwo = advantageTwo;
    return this;
  }

  public AdvantageMark setDisadvantageOne(int disadvantageOne) {
    this.disadvantageOne = disadvantageOne;
    return this;
  }

  public AdvantageMark setDisadvantageTwo(int disadvantageTwo) {
    this.disadvantageTwo = disadvantageTwo;
    return this;
  }

  public HashMap<String, Integer> toHashMap() {
    var result = new HashMap<String, Integer>();
    result.put("advantageOne", this.advantageOne);
    result.put("advantageTwo", this.advantageTwo);
    result.put("disadvantageOne", this.disadvantageOne);
    result.put("disadvantageTwo", this.disadvantageTwo);
    return result;
  }

  public List<LLMFreeTextMarkSchemeEntry> toMarkScheme() {
    return generateMarkScheme(
        Field.field().setName("advantageOne").setMarks(advantageOne),
        Field.field().setName("advantageTwo").setMarks(advantageTwo),
        Field.field().setName("disadvantageOne").setMarks(disadvantageOne),
        Field.field().setName("disadvantageTwo").setMarks(disadvantageTwo));
  }

  public String toJSON() {
    return String.format(
        "{\"advantageOne\": %d, \"advantageTwo\": %d, \"disadvantageOne\": %d, \"disadvantageTwo\": %d}",
        advantageOne, advantageTwo, disadvantageOne, disadvantageTwo);
  }
}
