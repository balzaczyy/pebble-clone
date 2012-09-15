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
package net.sourceforge.pebble.util;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.PebbleContext;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.security.PebbleUserDetails;
import net.sourceforge.pebble.security.SecurityRealm;
import net.sourceforge.pebble.security.SecurityRealmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.zhouyiyan.pebble.User;

/**
 * A collection of utility methods for security.
 *
 * @author    Simon Brown
 */
public final class SecurityUtils {
  private static final Log log = LogFactory.getLog(SecurityUtils.class);

	public static String getUsername() {
		return User.current().getName();
  }

	public static PebbleUserDetails getUserDetails() {
    try {
      SecurityRealm realm = PebbleContext.getInstance().getConfiguration().getSecurityRealm();
			return realm.getUser(getUsername());
    } catch (SecurityRealmException e) {
      log.error("Exception encountered", e);
      return null;
    }
  }

  public static boolean isUserInRole(String role) {
		return User.current().hasRole(role);
  }

  /**
   * Determines whether this user is a Pebble admin user.
   *
   * @return  true if the user is a Pebble admin, false otherwise
   */
  public static boolean isBlogAdmin() {
    return isUserInRole(Constants.BLOG_ADMIN_ROLE);
  }

  /**
   * Determines whether this user is a blog owner.
   *
   * @return  true if the user is a blog owner, false otherwise
   */
  public static boolean isBlogOwner() {
    return isUserInRole(Constants.BLOG_OWNER_ROLE);
  }

  /**
   * Determines whether this user is a blog publisher.
   *
   * @return  true if the user is a blog publisher, false otherwise
   */
  public static boolean isBlogPublisher() {
    return isUserInRole(Constants.BLOG_PUBLISHER_ROLE);
  }

  /**
   * Determines whether this user is a blog contributor.
   *
   * @return  true if the user is a blog contributor, false otherwise
   */
	private static boolean isBlogContributor() {
    return isUserInRole(Constants.BLOG_CONTRIBUTOR_ROLE);
  }

  public static void runAsBlogOwner() {
		User.login(User.mock("username", "password", Constants.BLOG_OWNER_ROLE));
  }

  public static void runAsBlogPublisher() {
		User.login(User.mock("username", "password", Constants.BLOG_PUBLISHER_ROLE));
  }

  public static void runAsBlogContributor() {
		User.login(User.mock("username", "password", Constants.BLOG_CONTRIBUTOR_ROLE));
  }

  public static void runAsAnonymous() {
		User.login(User.mock("username", "password"));
  }

  public static void runAsUnauthenticated() {
		User.logout();
  }

	public static boolean isUserAuthorisedForBlogAsBlogOwner(Blog blog, String username) {
		return isBlogOwner() && blog.isUserInRole(Constants.BLOG_OWNER_ROLE, username);
  }

	public static boolean isUserAuthorisedForBlogAsBlogPublisher(Blog blog, String username) {
		return isBlogPublisher() && blog.isUserInRole(Constants.BLOG_PUBLISHER_ROLE, username);
  }

	public static boolean isUserAuthorisedForBlogAsBlogContributor(Blog blog, String username) {
		return isBlogContributor() && blog.isUserInRole(Constants.BLOG_CONTRIBUTOR_ROLE, username);
  }

	public static boolean isUserAuthorisedForBlog(Blog blog) {
		String currentUser = getUsername();
		return isUserAuthorisedForBlogAsBlogOwner(blog, currentUser) //
				|| isUserAuthorisedForBlogAsBlogPublisher(blog, currentUser) //
				|| isUserAuthorisedForBlogAsBlogContributor(blog, currentUser);
  }

  public static boolean isUserAuthenticated() {
		return User.isAuthenticated();
  }

	// public static void main(String[] args) {
	// if (args.length != 3) {
	// System.out.println("Usage : [md5|sha|plaintext] username password");
	// } else if (args[0].equals("md5")) {
	// PasswordEncoder encoder = new Md5PasswordEncoder();
	// System.out.println(encoder.encodePassword(args[2], args[1]));
	// } else if (args[0].equals("sha")) {
	// PasswordEncoder encoder = new ShaPasswordEncoder();
	// System.out.println(encoder.encodePassword(args[2], args[1]));
	// } else if (args[0].equals("plaintext")) {
	// PasswordEncoder encoder = new PlaintextPasswordEncoder();
	// System.out.println(encoder.encodePassword(args[2], args[1]));
	// } else {
	// System.out.println("Algorithm must be md5, sha or plaintext");
	// }
	// }

}