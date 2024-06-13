package uk.ac.cam.cl.dtg.isaac.quiz;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkedExample;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dos.content.StringChoice;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class IsaacLLMFreeTextValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacLLMFreeTextValidator.class);

    private static final String MARKS_AWARDED_FIELD_NAME = "marksAwarded";
    private final AbstractConfigLoader configLoader;
    private final ObjectMapper mapper;
    private final OpenAIClient openAIClient;

    public IsaacLLMFreeTextValidator(final AbstractConfigLoader configLoader, final OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
        this.configLoader = configLoader;
        this.mapper = new ObjectMapper();
    }

    private static void validateInputs(final Question question, final Choice answer) {
        Objects.requireNonNull(question);
        Objects.requireNonNull(answer);
        if (!(question instanceof IsaacLLMFreeTextQuestion)) {
            throw new IllegalArgumentException(question.getId() + " is not a LLM free-text question");
        }
        if (!(answer instanceof StringChoice)) {
            throw new IllegalArgumentException(
                    answer.getClass() + " is not of expected type StringChoice for (" + question.getId() + ")");
        }
    }

    private String generatePromptSystemMessage(final IsaacLLMFreeTextQuestion question) {
        String contextAndInstructions = "You are a principal examiner marking A Level and GCSE "
            + String.format("%s questions.\n", this.configLoader.getProperty(LLM_MARKER_SUBJECT))
            + "You do not like vagueness in student answers.\n"
            + "You follow a standard procedure when marking a question attempt and write your responses as JSON:\n"
            + "```\n"
            + "# QUESTION MARKING PROCEDURE\n"
            + "if the attempt is similar enough to an attempt that you've marked before:\n"
            + "  award the student the same json marks as you gave in that prior example.\n"
            + "else:\n"
            + "  for each mark in the mark scheme:\n"
            + "    if the attempt meets the criteria of the mark: # taking into consideration any ADDITIONAL MARKING INSTRUCTIONS\n"
            + "      record an entry in the JSON response indicating the field and the numeric mark value, i.e. `\"abbreviatedSnakeCaseMarkDescriptor\": 1`.\n"
            + "\n"
            + String.format("finally add a field `%s` value to the JSON object ", MARKS_AWARDED_FIELD_NAME)
            + "which takes the minimum value from the sum of the awarded marks or the question's `maxMarks`.\n"
            + "```\n"
            + "Here is the question and mark scheme that you're currently marking against.\n\n";

        String questionText = "# QUESTION:\n" + question.getChildren().stream().filter(child -> child instanceof Content)
                .map(child -> ((Content) child).getValue()).collect(Collectors.joining("\n")) + "\n\n";

        String markScheme = "# MARK SCHEME:\n" + question.getMarkScheme().stream()
                .map(mark -> mark.getJsonField() + ": " + mark.getShortDescription())
                .collect(Collectors.joining("\n")) + "\n\n";

        String additionalMarkingInstructions = question.getAdditionalMarkingInstructions() != null ?
                String.format("# ADDITIONAL MARKING INSTRUCTIONS:\n%s\n\n", question.getAdditionalMarkingInstructions()) : "";

        String maxMarks = "# MAX MARKS: " + question.getMaxMarks() + "\n\n";

        return contextAndInstructions + questionText + markScheme + additionalMarkingInstructions + maxMarks;
    }

    private String reportMarksAsJsonString(final Map<String, Integer> markBreakdown, Integer marksAwarded) {
        try {
            Map<String, Integer> marks = new HashMap<>(markBreakdown);
            marks.put(MARKS_AWARDED_FIELD_NAME, marksAwarded);
            return mapper.writeValueAsString(marks);
        } catch (JsonProcessingException e) { // TODO MT perhaps throw an exception here for the content team
            log.error("Failed to generate JSON from example marks in content - should never happen", e);
            return "{}";
        }
    }

    private List<ChatRequestMessage> generateQuestionPrompt(final IsaacLLMFreeTextQuestion question) {
        List<ChatRequestMessage> chatMessages = new ArrayList<>();

        // Add system message specifying question and mark scheme
        chatMessages.add(new ChatRequestSystemMessage(generatePromptSystemMessage(question)));

        // Add N-shot examples
        for (LLMFreeTextMarkedExample example : question.getMarkedExamples()) {
            chatMessages.add(new ChatRequestUserMessage(example.getAnswer()));
            chatMessages.add(new ChatRequestAssistantMessage(
                    reportMarksAsJsonString(example.getMarks(), example.getMarksAwarded())));
        }

        return chatMessages;
    }

    private ChatRequestMessage extractUserAttemptAtQuestion(final Choice answer) {
        return new ChatRequestUserMessage(answer.getValue());
    }

    private Map<String, Integer> extractValidatedResponse(
            final IsaacLLMFreeTextQuestion question, final ChatCompletions chatCompletions) {
        if (chatCompletions.getChoices().size() != 1) {  // TODO MT throw a more useful user exception
            throw new IllegalStateException("Expected exactly one choice from the API, got: " + chatCompletions.getChoices().size());
        }
        String llmResponse = chatCompletions.getChoices().get(0).getMessage().getContent();

        try {
            Map<String, Object> response = this.mapper.readValue(llmResponse, new TypeReference<HashMap<String, Object>>() {});
            List<String> validMarkJsonFields = question.getMarkScheme().stream()
                    .map(LLMFreeTextMarkSchemeEntry::getJsonField).collect(Collectors.toList());
            validMarkJsonFields.add(MARKS_AWARDED_FIELD_NAME);
            Map<String, Integer> validatedMarks = Maps.newHashMap();
            for (String validMarkJsonField : validMarkJsonFields) {
                validatedMarks.put(validMarkJsonField, (Integer) response.getOrDefault(validMarkJsonField, 0));
            }

            return validatedMarks;
        } catch (JsonProcessingException e) { // TODO MT throw a more useful user exception
            log.error("Failed to parse response from OpenAI API: " + llmResponse, e);
            throw new IllegalArgumentException("Failed to parse JSON response", e);
        }
    }

    private Content generateFeedback(final IsaacLLMFreeTextQuestion question, final Map<String, Integer> validatedMarks) {
        try {
            Content feedback = new Content();
            feedback.setValue(mapper.writeValueAsString(validatedMarks) + "\n" +
                    "You scored " + validatedMarks.get(MARKS_AWARDED_FIELD_NAME) + " out of " + question.getMaxMarks());
            return feedback;
        } catch (JsonProcessingException e) { // Should not be possible in practise
            log.error("Failed to generate feedback from validated marks - should not be possible.", e);
            throw new IllegalArgumentException("Failed to generate feedback from validated marks", e);
        }
    }

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        validateInputs(question, answer);
        IsaacLLMFreeTextQuestion freeTextLLMQuestion = (IsaacLLMFreeTextQuestion) question;

        List<ChatRequestMessage> questionPrompt = generateQuestionPrompt(freeTextLLMQuestion);

        questionPrompt.add(extractUserAttemptAtQuestion(answer));

        ChatCompletions chatCompletions = openAIClient.getChatCompletions(
                configLoader.getProperty(LLM_MARKER_DEFAULT_MODEL_NAME),
                new ChatCompletionsOptions(questionPrompt).setTemperature(0.0));

        Map<String, Integer> validatedMarks = extractValidatedResponse(freeTextLLMQuestion, chatCompletions);
        boolean isCorrect = validatedMarks.getOrDefault(MARKS_AWARDED_FIELD_NAME, 0) > 0;

        // TODO MT return a new feedback subtype that preserves more structured information for front-end rendering
        Content feedback = generateFeedback(freeTextLLMQuestion, validatedMarks);

        return new QuestionValidationResponse(question.getId(), answer, isCorrect, feedback, new Date());
    }
}
