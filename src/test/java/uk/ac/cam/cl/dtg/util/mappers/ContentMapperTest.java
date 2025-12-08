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

public class ContentMapperTest {
    private final MainMapper mapper = MainMapper.INSTANCE;

    @SuppressWarnings("unchecked")
    private static Stream<Arguments> testCasesDOtoDTO() {
        Reflections reflections = new Reflections("uk.ac.cam.cl.dtg.isaac.dos");
        Set<Class<? extends Content>> contentSubclasses = reflections.getSubTypesOf(Content.class);
        contentSubclasses.add(Content.class);

        return contentSubclasses.stream()
                .map(subclass -> {
                    if (Modifier.isAbstract(subclass.getModifiers())) {
                        return null;
                    }
                    Class<? extends ContentDTO> dtoClass = (Class<? extends ContentDTO>) subclass.getAnnotation(DTOMapping.class).value();
                    return dtoClass != null ? Arguments.of(subclass, dtoClass) : null;
                })
                .filter(Objects::nonNull);
    }

    @ParameterizedTest
    @MethodSource("testCasesDOtoDTO")
    void testDOtoDTOMapping(final Class<? extends Content> sourceDOClass, final Class<? extends ContentDTO> expectedDTOClass) {
        Content source;
        try {
            source = sourceDOClass.getConstructor().newInstance();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to instantiate source class: " + sourceDOClass.getName(), e);
        }
        Assertions.assertEquals(expectedDTOClass, mapper.map(source).getClass());
    }
}
