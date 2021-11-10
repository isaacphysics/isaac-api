package uk.ac.cam.cl.dtg.segue.dao.content;


import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

/**
 * An Orika BidirectionalConverter that implements the old, polymorphic, canConvert check.
 *
 * @param <S> - source type
 * @param <D> - destination type
 */
public abstract class AbstractPolymorphicBidirectionalConverter<S, D> extends BidirectionalConverter<S, D> {

    @Override
    public boolean canConvert(Type<?> sourceType, Type<?> destinationType) {
        /* The behaviour of canConvert changed in Orika v1.5.0 to only convert exact class matches,
           to fix an issue with converters acting too loosely on pairs of classes they were not
           meant to convert.
           The crux of the change was swapping from sourceType.isAssignableFrom() to sourceType.equals(),
           which for our use case prevents the clever polymorphism we do with subtypes working in converters.

           Since this is a bidirectional converter, we must check whether the reverse conversion is
           possible too. It is not immediately obvious what the correct reverse check must be, and so this
           check has been copied from the old Orika code.

           See the commit that changed this behaviour here:
           https://github.com/orika-mapper/orika/commit/554396579c96b3356c3c31ceb2e236cba0ffbaba
         */
        boolean forwardConvertable = this.sourceType.isAssignableFrom(sourceType) && this.destinationType.equals(destinationType);
        boolean reverseConvertable = this.destinationType.isAssignableFrom(sourceType) && this.sourceType.equals(destinationType);
        return forwardConvertable || reverseConvertable;
    }
}
