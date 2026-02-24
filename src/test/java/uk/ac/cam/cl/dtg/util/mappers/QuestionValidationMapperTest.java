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

/**
 * Parameterised unit tests verifying mapping from subclasses of {@code QuestionValidationResponse} to their
 * corresponding {@code QuestionValidationResponseDTO} subclasses.
 */
public class QuestionValidationMapperTest {
    private final MainMapper mapper = MainMapper.INSTANCE;

    /**
     * Builds parameterised test cases mapping each {@code QuestionValidationResponse} subclass to its
     * corresponding {@code QuestionValidationResponseDTO} class.
     *
     * @return a stream of {@code QuestionValidationResponse}/{@code QuestionValidationResponseDTO} class pairings
     */
    public static Stream<Arguments> testCasesDOtoDTO() {
        Reflections reflections = new Reflections("uk.ac.cam.cl.dtg.isaac.dos");
        Set<Class<? extends QuestionValidationResponse>> subclasses = reflections.getSubTypesOf(QuestionValidationResponse.class);

        return subclasses.stream()
                .map(subclass -> {
                    DTOMapping mapping = subclass.getAnnotation(DTOMapping.class);
                    if (mapping == null) {
                        return null;
                    }
                    return Arguments.of(subclass, mapping.value());
                })
                .filter(Objects::nonNull);
    }

    @ParameterizedTest
    @MethodSource("testCasesDOtoDTO")
    public void testDOtoDTOMapping(final Class<? extends QuestionValidationResponse> sourceDOClass,
                            final Class<? extends QuestionValidationResponseDTO> expectedDTOClass) {
        QuestionValidationResponse source;
        try {
            source = sourceDOClass.getConstructor().newInstance();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to instantiate source class: " + sourceDOClass.getName(), e);
        }
        Assertions.assertEquals(expectedDTOClass, mapper.map(source).getClass());
    }
}
