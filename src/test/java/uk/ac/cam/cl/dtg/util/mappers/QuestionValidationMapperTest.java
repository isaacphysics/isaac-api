package uk.ac.cam.cl.dtg.util.mappers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class QuestionValidationMapperTest {
    private final MainMapper mapper = MainMapper.INSTANCE;

    @SuppressWarnings("unchecked")
    private static Stream<Arguments> testCasesDOtoDTO() {
        Reflections reflections = new Reflections("uk.ac.cam.cl.dtg.isaac.dos");
        Set<Class<? extends QuestionValidationResponse>> subclasses = reflections.getSubTypesOf(QuestionValidationResponse.class);
        subclasses.add(QuestionValidationResponse.class);

        return subclasses.stream()
                .map(subclass -> {
                    if (subclass.isAnnotationPresent(DTOMapping.class)) {
                        Class<? extends QuestionValidationResponseDTO> dtoClass = (Class<? extends QuestionValidationResponseDTO>) subclass.getAnnotation(DTOMapping.class).value();
                        return dtoClass != null ? Arguments.of(subclass, dtoClass) : null;
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    @ParameterizedTest
    @MethodSource("testCasesDOtoDTO")
    void testDOtoDTOMapping(final Class<? extends QuestionValidationResponse> sourceDOClass, final Class<? extends QuestionValidationResponse> expectedDTOClass) {
        QuestionValidationResponse source;
        try {
            source = sourceDOClass.getConstructor().newInstance();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to instantiate source class: " + sourceDOClass.getName(), e);
        }
        Assertions.assertEquals(expectedDTOClass, mapper.map(source).getClass());
    }
}
