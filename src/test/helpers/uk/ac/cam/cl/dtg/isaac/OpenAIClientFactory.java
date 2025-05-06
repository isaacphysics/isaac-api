package uk.ac.cam.cl.dtg.isaac;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.isA;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;

import java.util.Collections;

import org.easymock.EasyMock;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatResponseMessage;

public class OpenAIClientFactory {
  public static OpenAIClient client() {
    return client("");
  }

  public static OpenAIClient client(final String llmResponse) {
    // These must be PowerMocked since the classes are final in the Azure OpenAI
    // library

    var client = createMock(OpenAIClient.class);
    var chatCompletions = createMock(ChatCompletions.class);
    var chatChoice = createMock(ChatChoice.class);
    var chatResponseMessage = createMock(ChatResponseMessage.class);

    EasyMock.expect(chatResponseMessage.getContent()).andReturn(llmResponse);
    EasyMock.expect(chatChoice.getMessage()).andReturn(chatResponseMessage);
    EasyMock.expect(chatCompletions.getChoices()).andReturn(Collections.singletonList(chatChoice)).times(2);
    EasyMock.expect(client.getChatCompletions(anyString(), isA(ChatCompletionsOptions.class)))
        .andReturn(chatCompletions);

    replayAll();
    return client;
  }
}
