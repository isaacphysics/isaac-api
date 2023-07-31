/**
 * Copyright 2018 Meurig Thomas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.quiz;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLlmQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.LlmPrompt;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dos.content.StringChoice;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class IsaacLlmValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacLlmValidator.class);
    private final AbstractConfigLoader configLoader;
    private final ObjectMapper mapper;
    private final OpenAIClient openAIClient;
    private static final String DEFAULT_MODEL = "text-davinci-003";
    private static final String STUDENT_ANSWER_TEMPLATE_VAR = "<$STUDENT_ANSWER>";

    public IsaacLlmValidator(final AbstractConfigLoader configLoader, final OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
        this.configLoader = configLoader;
        this.mapper = new ObjectMapper();
    }

    private static void validateInputs(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacLlmQuestion)) {
            throw new IllegalArgumentException(question.getId() + " is not a LLM question");
        }

        if (!(answer instanceof StringChoice)) {
            throw new IllegalArgumentException(
                    answer.getClass() + " is not of expected type StringChoice for (" + question.getId() + ")");
        }

        // TODO Check that <$STUDENT_ANSWER> and other stop words are not present in the student's answer.
    }

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        validateInputs(question, answer);
        IsaacLlmQuestion isaacLlmQuestion = (IsaacLlmQuestion) question;

        boolean isCorrectResponse = false;
        Content feedback = null;
        for (Choice choice : isaacLlmQuestion.getChoices()) {
            if (choice instanceof LlmPrompt) {
                LlmPrompt llmPrompt = (LlmPrompt) choice;

                String prompt = new StringBuilder()
                        .append(llmPrompt.getValue().replace(STUDENT_ANSWER_TEMPLATE_VAR, answer.getValue()))
                        .append("\n")
                        .append("Use this JSON format:\n\n")
                        .append("{\n")
                        .append("  \"correct\": <true OR false>")
                        .append("}\n\n")
                        .append("Response:\n\n").toString();

                Completions completions = this.openAIClient.getCompletions(
                        DEFAULT_MODEL,
                        new CompletionsOptions(List.of(prompt)).setTemperature(0.0)
                );

                for (com.azure.ai.openai.models.Choice responseChoice : completions.getChoices()) {
                    log.debug(String.format("Index: %d, Text: %s.%n", responseChoice.getIndex(), responseChoice.getText()));
                    String responseString = responseChoice.getText();
                    try {
                        HashMap<String, Object> response = this.mapper.readValue(responseString, HashMap.class);
                        if (response.containsKey("correct")) {
                            isCorrectResponse = (boolean) response.get("correct");
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse response from OpenAI API: " + responseString, e);
                    }
                }
            } else {
                log.error("QuestionId: " + question.getId() + " contains a choice which is not a LlmPrompt.");
            }
        }

        // If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != isaacLlmQuestion.getDefaultFeedback()) {
            feedback = isaacLlmQuestion.getDefaultFeedback();
        }

        return new QuestionValidationResponse(question.getId(), answer, isCorrectResponse, feedback, new Date());
    }
}
