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
		if (User.authenticate(username, password)) {
			HttpServletRequest httpRequest = (HttpServletRequest) req;
			HttpSession session = httpRequest.getSession(true);
			session.setAttribute("username", username);
			String prefix = PebbleContext.getInstance().getConfiguration().getUrl();
			if (prefix.charAt(prefix.length() - 1) == '/') prefix = prefix.substring(0, prefix.length() - 1);
			HttpServletResponse response = (HttpServletResponse) resp;
			response.sendRedirect(prefix + redirectUrl);
		} else {
			HttpSession session = ((HttpServletRequest) req).getSession(false);
			if (session != null && (username = (String) session.getAttribute("username")) != null) {
				User.setCurrent(username);
			}
			try {
				chain.doFilter(req, resp);
			} finally {
				User.setCurrent(null);
			}
		}
	}

	public void destroy() {}
}
