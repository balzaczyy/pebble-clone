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

package net.sourceforge.pebble.web.tagext;

import java.lang.reflect.Method;
import java.util.Collection;

import net.sourceforge.pebble.util.StringUtils;
import net.sourceforge.pebble.util.UrlRewriter;
/**
 * Used from jsp taglib url.tld 
 * @author Olaf Kock
 * @see UrlRewriter
 */
public class UrlFunctions {
	public static String rewrite(String url) {
		// if you'd like to see the decorated urls highlighted, 
		// a html injection is commented. This wouldn't work 
		// for all cases, but gives an idea where rewriting an
		// url is done (and has succeeded)
		return UrlRewriter.doRewrite(url); // + "\" style=\"background-color:blue";
	}

	public static String escape(String url) {
		String ans = StringUtils.transformHTML(url);
		return ans == null ? "" : ans;
	}

	public String escapeOr(String url, String defaultValue) {
		return url == null ? defaultValue : escape(url);
	}

	public String or(String value, String other) {
		return value != null ? value : other;
	}

	public String createGravatar(String email) {
		return Util.createGravatar(email);
	}

	public Object getField(Object o, String value) throws Exception {
		if (value != null) {
			String name = "get" + value.substring(0, 1).toUpperCase() + value.substring(1);
			Method m = o.getClass().getMethod(name, new Class[] {});
			return m.invoke(o, new Object[] {}).toString();
		}
		return o.toString();
	}

	public boolean isCollection(Object o) {
		return o instanceof Collection;
	}
}
