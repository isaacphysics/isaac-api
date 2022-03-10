package uk.ac.cam.cl.dtg.segue.api;

import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.api.AbstractFacadeTest;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.segue.dos.UserPreference;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.junit.Assert.assertTrue;

public class UsersFacadeTest extends AbstractFacadeTest {

    private UsersFacade usersFacade;
    private ILogManager logManager;
    private UserAssociationManager associationManager;
    private IMisuseMonitor misuseMonitor;
    private AbstractUserPreferenceManager userPreferenceManager;
    private SchoolListReader schoolListReader;

    @Before
    public void setUp() throws SegueDatabaseException {

        // Create UsersFacade and mock dependencies
        PropertiesLoader properties = createMock(PropertiesLoader.class);
        logManager = createNiceMock(ILogManager.class);
        associationManager = createMock(UserAssociationManager.class);
        misuseMonitor = createMock(IMisuseMonitor.class);
        schoolListReader = createMock(SchoolListReader.class);
        userPreferenceManager = createMock(AbstractUserPreferenceManager.class);

        usersFacade = new UsersFacade(properties, userManager, logManager, associationManager, misuseMonitor,
                userPreferenceManager, schoolListReader);

        // Set up stub methods and return values for dependencies
        UserPreference preference = new UserPreference(1L, "type_a", "pref_a",
                true);
        List<UserPreference> mockUserPreferences = new ArrayList<>();
        mockUserPreferences.add(preference);
        expect(userPreferenceManager.getAllUserPreferences(1L)).andStubReturn(mockUserPreferences);

        replay(properties, logManager, associationManager, misuseMonitor, schoolListReader, userPreferenceManager);
    }

    @Test
    public void getUserPreferencesEndpoint_loggedOutUserRequest_respondsWithUnauthorized() {
        forEndpoint(() -> usersFacade.getUserPreferences(request),
            requiresLogin()
        );
    }

    @Test
    public void getUserPreferencesEndpoint_loggedInUserRequest_respondsWithUserPreferences() {
        forEndpoint(() -> usersFacade.getUserPreferences(request),
            as(student,
                check((response) -> assertTrue(((Map<?, ?>)response.getEntity()).containsKey("type_a")))
            )
        );
    }
}
