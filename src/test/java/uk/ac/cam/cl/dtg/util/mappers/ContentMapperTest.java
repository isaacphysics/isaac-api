package uk.ac.cam.cl.dtg.util.mappers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;

import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Parameterised unit tests verifying mapping from subclasses of {@code Content} to their corresponding
 * {@code ContentDTO} subclasses.
 */
class ContentMapperTest {
    private final MainMapper mapper = MainMapper.INSTANCE;

    /**
     * Builds parameterised test cases mapping each concrete {@code Content} subclass to its corresponding
     * {@code ContentDTO} class.
     *
     * @return a stream of {@code Content}/{@code ContentDTO} class pairings
     */
    static Stream<Arguments> testCasesDOtoDTO() {
        Reflections reflections = new Reflections("uk.ac.cam.cl.dtg.isaac.dos");
        Set<Class<? extends Content>> contentSubclasses = reflections.getSubTypesOf(Content.class);
        contentSubclasses.add(Content.class);

        return contentSubclasses.stream()
                .filter(subclass -> !Modifier.isAbstract(subclass.getModifiers()))
                .map(subclass -> {
                    if (Modifier.isAbstract(subclass.getModifiers())) {
                        return null;
                    }
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
    void testDOtoDTOMapping(final Class<? extends Content> sourceDOClass,
                            final Class<? extends ContentDTO> expectedDTOClass) {
        Content source;
        try {
            source = sourceDOClass.getConstructor().newInstance();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to instantiate source class: " + sourceDOClass.getName(), e);
        }
        Assertions.assertEquals(expectedDTOClass, mapper.map(source).getClass());
    }
}
