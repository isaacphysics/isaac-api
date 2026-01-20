package uk.ac.cam.cl.dtg.util.mappers;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reflections.Reflections;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parameterised unit tests verifying mapping from subclasses of {@code QuestionValidationResponse} to their
 * corresponding {@code QuestionValidationResponseDTO} subclasses.
 */
@RunWith(Parameterized.class)
public class QuestionValidationMapperTest {
    private final MainMapper mapper = MainMapper.INSTANCE;

    Class<? extends QuestionValidationResponse> sourceDOClass;
    Class<? extends QuestionValidationResponseDTO> expectedDTOClass;

    /**
     * Builds parameterised test cases mapping each {@code QuestionValidationResponse} subclass to its
     * corresponding {@code QuestionValidationResponseDTO} class.
     *
     * @return collection of {@code QuestionValidationResponse}/{@code QuestionValidationResponseDTO} class pairings
     */
    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Object[]> testCasesDOtoDTO() {
        Reflections reflections = new Reflections("uk.ac.cam.cl.dtg.isaac.dos");
        Set<Class<? extends QuestionValidationResponse>> subclasses = reflections.getSubTypesOf(QuestionValidationResponse.class);

        return subclasses.stream()
                .map(subclass -> {
                    DTOMapping mapping = subclass.getAnnotation(DTOMapping.class);
                    if (mapping == null) {
                        return null;
                    }
                    Class<? extends QuestionValidationResponseDTO> dtoClass = (Class<? extends QuestionValidationResponseDTO>) mapping.value();
                    return new Object[]{subclass, dtoClass};
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public QuestionValidationMapperTest(final Class<? extends QuestionValidationResponse> sourceDOClass,
                             final Class<? extends QuestionValidationResponseDTO> expectedDTOClass) {
        this.sourceDOClass = sourceDOClass;
        this.expectedDTOClass = expectedDTOClass;
    }

    @Test
    public void testDOtoDTOMapping() {
        QuestionValidationResponse source;
        try {
            source = sourceDOClass.getConstructor().newInstance();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to instantiate source class: " + sourceDOClass.getName(), e);
        }
        Assertions.assertEquals(expectedDTOClass, mapper.map(source).getClass());
    }
}
