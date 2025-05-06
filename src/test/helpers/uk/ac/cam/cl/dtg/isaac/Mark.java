package uk.ac.cam.cl.dtg.isaac;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;

public abstract class Mark {
  public static GenericMark mark() {
    return new GenericMark();
  }

  public static AdvantageMark advantageMark() {
    return new AdvantageMark();
  }

  public static PointMark pointMark() {
    return new PointMark();
  }

  public abstract List<LLMFreeTextMarkSchemeEntry> toMarkScheme();
  public abstract String toJSON();

  protected static List<LLMFreeTextMarkSchemeEntry> generateMarkScheme(Field... entries) {
    return Stream.of(entries).map(input -> {
              var output = new LLMFreeTextMarkSchemeEntry();
              output.setJsonField(input.name());
              output.setMarks(input.marks());
              return output;
            })
            .collect(Collectors.toList());
  }
}