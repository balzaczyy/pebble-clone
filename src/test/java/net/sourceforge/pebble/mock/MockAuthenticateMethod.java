package net.sourceforge.pebble.mock;

import cn.zhouyiyan.pebble.User;
import cn.zhouyiyan.pebble.User.AuthenticateMethod;

public class MockAuthenticateMethod implements AuthenticateMethod {
	private boolean grantAccess = true;
	private final String[] roles;

	public MockAuthenticateMethod(boolean grantAccess, String... roles) {
		this.grantAccess = grantAccess;
		this.roles = roles;
	}

	public User authenticate(String username, String password) {
		return grantAccess ? User.mock(username, password, roles) : null;
	}

}
