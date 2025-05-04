package uk.ac.cam.cl.dtg.isaac;

import java.util.HashMap;
import java.util.List;

import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;

public interface Mark {
  HashMap<String, Integer> toHashMap();
  List<LLMFreeTextMarkSchemeEntry> toMarkScheme();
  String toJSON();
};