package uk.ac.cam.cl.dtg.isaac.api.managers;

import uk.ac.cam.cl.dtg.isaac.IsaacTest;
import uk.ac.cam.cl.dtg.isaac.api.AbstractFacadeTest;

import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.verify;

abstract public class AbstractManagerTest extends IsaacTest {
    @SafeVarargs
    protected final <T, E extends Exception> void with(T mock, AbstractFacadeTest.SickConsumer<T, E>... setups) {
        verify(mock);
        reset(mock);
        try {
            for (AbstractFacadeTest.SickConsumer<T, E> setup: setups) {
                setup.accept(mock);
            }
        } catch (Exception e) {
            // This shouldn't happen.
            throw new RuntimeException("Error in mock setup", e);
        }
        replay(mock);
    }
}
