package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.time.Instant;

public class LongToInstantConverter extends StdConverter<Long, Instant> {
  @Override
  public Instant convert(final Long value) {
    return Instant.ofEpochMilli(value);
  }
}