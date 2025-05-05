package uk.ac.cam.cl.dtg.isaac;

import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextChoice;

public class AnswerFactory {
  public static LLMFreeTextChoice answer(String answerString) {
    LLMFreeTextChoice answer = new LLMFreeTextChoice();
    answer.setValue(answerString);
    return answer;
  }
}

