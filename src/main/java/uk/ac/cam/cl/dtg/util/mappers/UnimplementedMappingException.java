package uk.ac.cam.cl.dtg.util.mappers;

public class UnimplementedMappingException extends UnsupportedOperationException {
    public UnimplementedMappingException(Class<?> sourceClass, Class<?> targetClass) {
        super(String.format("Mapping from %s to %s is not implemented", sourceClass.getSimpleName(), targetClass.getSimpleName()));
    }
}
