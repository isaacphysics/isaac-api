package uk.ac.cam.cl.dtg.isaac.api.managers;

import uk.ac.cam.cl.dtg.isaac.IsaacTest;

import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.verify;

abstract public class AbstractManagerTest extends IsaacTest {
    @SafeVarargs
    protected final <T> void with(T mock, MockConfigurer<T>... setups) {
        verify(mock);
        reset(mock);
        if (defaultsMap.containsKey(mock)) {
            ((MockConfigurer<T>) defaultsMap.get(mock)).configure(mock);
        }
        for (MockConfigurer<T> setup: setups) {
            setup.configure(mock);
        }
        replay(mock);
    }
}
