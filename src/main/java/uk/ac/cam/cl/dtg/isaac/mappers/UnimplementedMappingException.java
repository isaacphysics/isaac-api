package uk.ac.cam.cl.dtg.isaac.mappers;

public class UnimplementedMappingException extends UnsupportedOperationException {

  public UnimplementedMappingException(Class<?> sourceClass, Class<?> targetClass) {
    super(String.format("Invocation of unimplemented mapping from %s to %s", sourceClass.getSimpleName(),
        targetClass.getSimpleName()));
  }
}
