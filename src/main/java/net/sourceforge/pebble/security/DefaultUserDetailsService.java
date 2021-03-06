/*
 * Copyright (c) 2003-2011, Simon Brown
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *   - Neither the name of Pebble nor the names of its contributors may
 *     be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.pebble.security;

import net.sourceforge.pebble.Configuration;

/**
 * Implementation of the UserDetailsService that gets authentication
 * credentials from a SecurityRealm implementation.
 *
 * @author    Simon Brown
 */
public class DefaultUserDetailsService {
  private final SecurityRealm securityRealm;

	public DefaultUserDetailsService(Configuration configuration) {
		this.securityRealm = new DefaultSecurityRealm(configuration);
	}

	public DefaultUserDetailsService(SecurityRealm securityRealm) {
		this.securityRealm = securityRealm;
	}

  /**
   * Looks up and returns user details for the given username.
   *
   * @param username    the username to find details for
   * @return  a PebbleUserDetails instance
   */
	public PebbleUserDetails loadUserByUsername(String username) {
    try {
      PebbleUserDetails user = securityRealm.getUser(username);
      if (user == null) {
				// throw new UsernameNotFoundException("A user with username " +
				// username + " does not exist");
      } else {
        return user;
      }
    } catch (SecurityRealmException e) {
			// throw new UsernameNotFoundException("User details not found", e);
    }
		return null;
  }

  public SecurityRealm getSecurityRealm() {
    return securityRealm;
  }
}