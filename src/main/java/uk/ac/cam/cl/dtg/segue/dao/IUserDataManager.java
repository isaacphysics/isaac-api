package uk.ac.cam.cl.dtg.segue.dao;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dto.users.User;

public interface IUserDataManager {

	public String register(User user, AuthenticationProvider provider,
			String providerId);

	public User getByLinkedAccount(AuthenticationProvider provider,
			String providerId);

	public User getById(String id) throws IllegalArgumentException;
}
