package uk.ac.cam.cl.dtg.util.mappers;

/**
 * An exception that indicates no mapping implementation exists between two classes.
 */
public class UnimplementedMappingException extends UnsupportedOperationException {
    public UnimplementedMappingException(final Class<?> sourceClass, final Class<?> targetClass) {
        super(String.format("Mapping from %s to %s is not implemented", sourceClass.getSimpleName(),
                targetClass.getSimpleName()));
    }
}
