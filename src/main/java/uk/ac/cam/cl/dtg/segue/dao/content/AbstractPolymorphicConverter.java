package uk.ac.cam.cl.dtg.segue.dao.content;


import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.metadata.Type;

/**
 * An Orika CustomConverter that implements the old, polymorphic, canConvert check.
 *
 * @param <S> - source type
 * @param <D> - destination type
 */
public abstract class AbstractPolymorphicConverter<S, D> extends CustomConverter<S, D> {

    @Override
    public boolean canConvert(Type<?> sourceType, Type<?> destinationType) {
        /* The behaviour of canConvert changed in Orika v1.5.0 to only convert exact class matches,
           to fix an issue with converters acting too loosely on pairs of classes they were not
           meant to convert.
           The crux of the change was swapping from sourceType.isAssignableFrom() to sourceType.equals(),
           which for our use case prevents the clever polymorphism we do with subtypes working in converters.

           See the commit that changed this behaviour here:
           https://github.com/orika-mapper/orika/commit/554396579c96b3356c3c31ceb2e236cba0ffbaba
         */
        return this.sourceType.isAssignableFrom(sourceType) && this.destinationType.equals(destinationType);
    }
}
