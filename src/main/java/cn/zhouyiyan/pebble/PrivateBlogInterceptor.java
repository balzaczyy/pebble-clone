package cn.zhouyiyan.pebble;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.PebbleContext;
import net.sourceforge.pebble.domain.AbstractBlog;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.util.SecurityUtils;

public final class PrivateBlogInterceptor implements Filter {
	public void init(FilterConfig config) throws ServletException {}

	public void doFilter(ServletRequest request, ServletResponse resp, //
			FilterChain chain) throws IOException, ServletException {
		String uri = (String) request.getAttribute(Constants.INTERNAL_URI);
		boolean isPublicResource = uri.endsWith("loginPage.action") //
				|| uri.endsWith(".secureaction") //
				|| uri.startsWith("/themes/") //
				|| uri.startsWith("/scripts/") //
				|| uri.startsWith("/common/") //
				|| uri.startsWith("/dwr/") //
				|| uri.equals("/robots.txt") //
				|| uri.equals("/pebble.css") //
				|| uri.equals("/favicon.ico") //
				|| uri.startsWith("/FCKeditor/");
		if (!isPublicResource) {
			AbstractBlog ab = (AbstractBlog) request.getAttribute(Constants.BLOG_KEY);
			if (ab instanceof Blog) {
				Blog blog = (Blog) ab;
				if (blog.isPrivate() && !SecurityUtils.isUserAuthorisedForBlog(blog)) {
					String prefix = PebbleContext.getInstance().getConfiguration().getUrl();
					HttpServletResponse response = (HttpServletResponse) resp;
					response.sendRedirect(prefix + "loginPage.action?error=error.notAuthorised");
					return;
				}
			}
		}
		chain.doFilter(request, resp);
	}

	public void destroy() {}
}
