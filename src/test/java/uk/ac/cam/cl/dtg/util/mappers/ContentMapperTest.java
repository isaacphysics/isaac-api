package uk.ac.cam.cl.dtg.util.mappers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reflections.Reflections;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parameterised unit tests verifying mapping from subclasses of {@code Content} to their corresponding
 * {@code ContentDTO} subclasses.
 */
@RunWith(Parameterized.class)
public class ContentMapperTest {
    private final MainMapper mapper = MainMapper.INSTANCE;

    Class<? extends Content> sourceDOClass;
    Class<? extends ContentDTO> expectedDTOClass;

    /**
     * Builds parameterised test cases mapping each concrete {@code Content} subclass to its corresponding
     * {@code ContentDTO} class.
     *
     * @return collection of {@code Content}/{@code ContentDTO} class pairings
     */
    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Object[]> testCasesDOtoDTO() {
        Reflections reflections = new Reflections("uk.ac.cam.cl.dtg.isaac.dos");
        Set<Class<? extends Content>> contentSubclasses = reflections.getSubTypesOf(Content.class);
        contentSubclasses.add(Content.class);

        return contentSubclasses.stream()
                .map(subclass -> {
                    if (Modifier.isAbstract(subclass.getModifiers())) {
                        return null;
                    }
                    DTOMapping mapping = subclass.getAnnotation(DTOMapping.class);
                    if (mapping == null) {
                        return null;
                    }
                    Class<? extends ContentDTO> dtoClass = (Class<? extends ContentDTO>) mapping.value();
                    return new Object[]{subclass, dtoClass};
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public ContentMapperTest(final Class<? extends Content> sourceDOClass,
                             final Class<? extends ContentDTO> expectedDTOClass) {
        this.sourceDOClass = sourceDOClass;
        this.expectedDTOClass = expectedDTOClass;
    }

    @Test
    public void testDOtoDTOMapping() {
        Content source;
        try {
            source = sourceDOClass.getConstructor().newInstance();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to instantiate source class: " + sourceDOClass.getName(), e);
        }
        Assertions.assertEquals(expectedDTOClass, mapper.map(source).getClass());
    }
}
