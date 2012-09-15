package cn.zhouyiyan.pebble;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.pebble.PebbleContext;

public class AuthenticationFilter implements Filter {
	public void init(FilterConfig config) throws ServletException {}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
			ServletException {
		String username = req.getParameter("j_username");
		String password = req.getParameter("j_password");
		String redirectUrl = req.getParameter("redirectUrl");
		if (username == null) {
			HttpSession session = ((HttpServletRequest) req).getSession(false);
			if (session != null && (username = (String) session.getAttribute("username")) != null) {
				User.login(username);
			}
			try {
				chain.doFilter(req, resp);
			} finally {
				User.logout();
			}
		} else if (User.authenticate(username, password)) { // login success
			// persist the identity in session
			HttpServletRequest httpRequest = (HttpServletRequest) req;
			HttpSession session = httpRequest.getSession(true);
			session.setAttribute("username", username);
			// redirect to given URL
			String prefix = PebbleContext.getInstance().getConfiguration().getUrl();
			if (prefix.charAt(prefix.length() - 1) == '/') prefix = prefix.substring(0, prefix.length() - 1);
			HttpServletResponse response = (HttpServletResponse) resp;
			response.sendRedirect(prefix + redirectUrl);
		} else { // login fail
			String prefix = PebbleContext.getInstance().getConfiguration().getUrl();
			HttpServletResponse response = (HttpServletResponse) resp;
			response.sendRedirect(prefix + "loginPage.action?error=login.incorrect");
			// TODO consider "openid.not.mapped" and "openid.error"
		}
	}

	public void destroy() {}
}
