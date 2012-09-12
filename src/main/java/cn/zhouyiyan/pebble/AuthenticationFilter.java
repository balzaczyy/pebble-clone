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

import net.sourceforge.pebble.Configuration;
import net.sourceforge.pebble.PebbleContext;
import net.sourceforge.pebble.security.DefaultUserDetailsService;

import org.springframework.security.authentication.dao.ReflectionSaltSource;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public class AuthenticationFilter implements Filter {
	private static final PasswordEncoder passwordEncoder = new ShaPasswordEncoder();
	private static final SaltSource saltSource = new ReflectionSaltSource();

	public void init(FilterConfig config) throws ServletException {}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
			ServletException {
		String username = req.getParameter("j_username");
		String password = req.getParameter("j_password");
		String redirectUrl = req.getParameter("redirectUrl");
		if (username != null || password != null) {
			Configuration conf = PebbleContext.getInstance().getConfiguration();
			UserDetailsService uds = new DefaultUserDetailsService(conf);
			UserDetails ud = uds.loadUserByUsername(username);
			String passtext = passwordEncoder.encodePassword(ud.getPassword(), saltSource.getSalt(ud));
			if (password.equals(passtext)) {
				HttpSession session = ((HttpServletRequest) req).getSession(true);
				session.setAttribute("username", username);
				HttpServletResponse response = (HttpServletResponse) resp;
				response.sendRedirect(redirectUrl);
				return;
			}
		}
		chain.doFilter(req, resp);
	}

	public void destroy() {}

}
