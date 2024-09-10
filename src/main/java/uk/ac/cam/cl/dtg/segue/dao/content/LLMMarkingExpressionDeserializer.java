package uk.ac.cam.cl.dtg.segue.dao.content;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingConstant;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingExpression;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingFunction;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingVariable;

import java.io.IOException;

public class LLMMarkingExpressionDeserializer extends JsonDeserializer<LLMMarkingExpression> {
    @Override
    public LLMMarkingExpression deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
            throws IOException {
        ObjectMapper objectMapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = objectMapper.readTree(jsonParser);

        if (null == root.get("type")) {
            throw JsonMappingException.from(
                    jsonParser, "Error: unable to parse content - no type property within the json input.");
        }

        String contentType = root.get("type").textValue();

        switch (contentType) {
            case "LLMMarkingFunction":
                return objectMapper.readValue(root.toString(), LLMMarkingFunction.class);
            case "LLMMarkingVariable":
                return objectMapper.readValue(root.toString(), LLMMarkingVariable.class);
            case "LLMMarkingConstant":
                return objectMapper.readValue(root.toString(), LLMMarkingConstant.class);
            default:
                throw JsonMappingException.from(
                        jsonParser,
                        String.format("Error: unable to parse LLM marking expression. Unhandled type: %s", contentType));
        }
    }

}