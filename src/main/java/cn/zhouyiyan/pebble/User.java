package cn.zhouyiyan.pebble;

import static cn.zhouyiyan.pebble.Predef.set;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.pebble.Configuration;
import net.sourceforge.pebble.PebbleContext;
import net.sourceforge.pebble.security.DefaultUserDetailsService;
import net.sourceforge.pebble.security.PebbleUserDetails;
import net.sourceforge.pebble.security.SecurityRealm;
import net.sourceforge.pebble.security.SecurityRealmException;

import org.jasypt.util.password.BasicPasswordEncryptor;

public class User {
	private static final User UnauthenticatedUser = new User(null) {};
	private static final ThreadLocal<User> current = new ThreadLocal<User>() {
		@Override
		protected User initialValue() {
			return UnauthenticatedUser;
		}
	};

	/**
	 * @return current authenticated user
	 */
	public static User current() {
		return current.get();
	}

	/**
	 * Set current authenticated user by name
	 */
	public static void login(String username) {
		current.set(User.byName(username));
	}

	public static void login(User user) {
		current.set(user != null ? user : UnauthenticatedUser);
	}

	public static void logout() {
		current.set(UnauthenticatedUser);
	}

	public static boolean isAuthenticated() {
		return current.get() != UnauthenticatedUser;
	}

	/**
	 * Obtain user by name.
	 */
	public static User byName(String username) {
		if (username != null) try {
			SecurityRealm realm = PebbleContext.getInstance().getConfiguration().getSecurityRealm();
			Collection<String> gas = realm.getUser(username).getAuthorities();
			String[] roles = new String[gas.size()];
			int idx = 0;
			for (String ga : gas) {
				roles[idx++] = ga;
			}
			User ans = new User(username);
			ans.roles.addAll(Arrays.asList(roles));
			return ans;
		} catch (SecurityRealmException sre) {
			sre.printStackTrace();
		}
		return UnauthenticatedUser;
	}

	/**
	 * Mock a user with given name, password, and roles.
	 */
	public static User mock(String username, String password, String... roles) {
		return new User(username, set(roles));
	}

	public interface AuthenticateMethod {
		User authenticate(String username, String password);
	}

	private static final BasicPasswordEncryptor passwordEncryptor = new BasicPasswordEncryptor();

	/**
	 * Default authenticate method.
	 */
	private static AuthenticateMethod authenticateMethod = new AuthenticateMethod() {
		public User authenticate(String username, String password) {
			if (username != null && password != null) {
				Configuration conf = PebbleContext.getInstance().getConfiguration();
				DefaultUserDetailsService uds = new DefaultUserDetailsService(conf);
				PebbleUserDetails ud = uds.loadUserByUsername(username);
				if (ud != null && passwordEncryptor.checkPassword(password, ud.getPassword())) { //
					return User.byName(username);
				}
			}
			return null;
		}
	};

	public static boolean authenticate(String username, String password) {
		User user = authenticateMethod.authenticate(username, password);
		if (user != null) current.set(user);
		return user != null;
	}

	/**
	 * Test-only.
	 */
	public static void setAuthenticateMethod(AuthenticateMethod method) {
		authenticateMethod = method;
	}

	private final String username;
	private final Set<String> roles = new HashSet<String>();

	private User(String username) {
		this.username = username;
	}

	public User(String username, Set<String> roles) {
		this.username = username;
		this.roles.addAll(roles);
	}

	public String getName() {
		return username;
	}

	public boolean hasRole(String role) {
		return roles.contains(role);
	}

}
