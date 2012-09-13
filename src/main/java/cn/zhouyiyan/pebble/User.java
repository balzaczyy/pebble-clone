package cn.zhouyiyan.pebble;

import static cn.zhouyiyan.pebble.Predef.set;

import java.util.HashSet;
import java.util.Set;

import net.sourceforge.pebble.Configuration;
import net.sourceforge.pebble.PebbleContext;
import net.sourceforge.pebble.security.DefaultUserDetailsService;

import org.springframework.security.authentication.dao.ReflectionSaltSource;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

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
		return username != null ? new User(username) : UnauthenticatedUser;
	}

	/**
	 * Mock a user with given name, password, and roles.
	 */
	public static User mock(String username, String password, String... roles) {
		return new User(username, password, set(roles));
	}

	public interface AuthenticateMethod {
		User authenticate(String username, String password);
	}

	private static final PasswordEncoder passwordEncoder = new ShaPasswordEncoder();
	private static final SaltSource saltSource = new ReflectionSaltSource();
	static {
		((ReflectionSaltSource) saltSource).setUserPropertyToUse("getUsername");
	}
	/**
	 * Default authenticate method.
	 */
	private static AuthenticateMethod authenticateMethod = new AuthenticateMethod() {
		public User authenticate(String username, String password) {
			if (username != null && password != null) {
				Configuration conf = PebbleContext.getInstance().getConfiguration();
				UserDetailsService uds = new DefaultUserDetailsService(conf);
				UserDetails ud = uds.loadUserByUsername(username);
				if (ud != null && ud.getPassword().equals(passwordEncoder.encodePassword(password, saltSource.getSalt(ud)))) { //
					return User.byName(username);
				}
			}
			return null;
		}
	};

	public static boolean authenticate(String username, String password) {
		User user = authenticateMethod.authenticate(username, password);
		current.set(user);
		return user != null;
	}

	/**
	 * Test-only.
	 */
	public static void setAuthenticateMethod(AuthenticateMethod method) {
		authenticateMethod = method;
	}

	private final String username;
	private final String password;
	private final Set<String> roles = new HashSet<String>();

	private User(String username) {
		this.username = username;
		this.password = null;
	}

	public User(String username, String password, Set<String> roles) {
		this.username = username;
		this.password = password;
		this.roles.addAll(roles);
	}

	public String getName() {
		return username;
	}

	public boolean hasRole(String role) {
		return roles.contains(role);
	}

}
