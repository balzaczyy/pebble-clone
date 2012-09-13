package cn.zhouyiyan.pebble;

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
	public static void setCurrent(String username) {
		if (username == null) {
			current.set(UnauthenticatedUser);
		} else {
			current.set(User.byName(username));
		}
	}

	/**
	 * Obtain user by name.
	 */
	public static User byName(String username) {
		return new User(username);
	}

	private static final PasswordEncoder passwordEncoder = new ShaPasswordEncoder();
	private static final SaltSource saltSource = new ReflectionSaltSource();
	static {
		((ReflectionSaltSource) saltSource).setUserPropertyToUse("getUsername");
	}

	public static boolean authenticate(String username, String password) {
		if (username == null || password == null) return false;
		Configuration conf = PebbleContext.getInstance().getConfiguration();
		UserDetailsService uds = new DefaultUserDetailsService(conf);
		UserDetails ud = uds.loadUserByUsername(username);
		return ud != null && ud.getPassword().equals(passwordEncoder.encodePassword(password, saltSource.getSalt(ud)));
	}

	private final String username;

	private User(String username) {
		this.username = username;
	}

	public String getName() {
		return username;
	}


}
