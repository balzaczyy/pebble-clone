package cn.zhouyiyan.pebble;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.PebbleContext;
import net.sourceforge.pebble.domain.AbstractBlog;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.util.SecurityUtils;
import net.sourceforge.pebble.web.security.SecurityTokenValidatorImpl;
import net.sourceforge.pebble.web.tagext.CalendarTag;
import net.sourceforge.pebble.web.tagext.UrlFunctions;

import org.apache.commons.io.FilenameUtils;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.servlet.VelocityServlet;

public class VelocityTemplateServlet extends VelocityServlet {
	private static final long serialVersionUID = 2560897251673033438L;
	private static final Map<Locale, List<String>> days = new HashMap<Locale, List<String>>();

	@Override
	protected Properties loadConfiguration(ServletConfig config) throws IOException, FileNotFoundException {
		Properties props = super.loadConfiguration(config);
		props.setProperty("file.resource.loader.path",
				"src/main/webapp/WEB-INF/templates,src/main/webapp/themes/default,src/main/webapp");
		return props;
	}

	@Override
	protected Template handleRequest(HttpServletRequest request, HttpServletResponse response, Context ctx)
			throws Exception {
		ctx.put("request", request);
		ctx.put("contextPath", request.getContextPath());

		// Security
		ctx.put("token", request.getAttribute(SecurityTokenValidatorImpl.PEBBLE_SECURITY_TOKEN_PARAMETER));
		ctx.put("isLoginPage", request.getAttribute("isLoginPage"));
		ctx.put(Constants.AUTHENTICATED_USER, request.getAttribute(Constants.AUTHENTICATED_USER));
		// rememberMe

		// Blog properties
		ctx.put("pebbleContext", PebbleContext.getInstance());
		AbstractBlog blog = (AbstractBlog) request.getAttribute(Constants.BLOG_KEY);
		ctx.put("blog", blog);
		ctx.put("blogUrl", blog.getUrl()); // TODO remove
		ctx.put("blogType", request.getAttribute(Constants.BLOG_TYPE));
		ctx.put("isAuthenticated", SecurityUtils.isUserAuthenticated());
		if (blog instanceof Blog) {
			ctx.put("archives", ((Blog) blog).getArchives());
			ctx.put("isBlogContributor",
					SecurityUtils.isUserAuthorisedForBlogAsBlogContributor((Blog) blog, SecurityUtils.getUsername()));
			ctx.put("isAuthorisedForBlog", SecurityUtils.isUserAuthorisedForBlog((Blog) blog));
			ctx.put("isBlogPublisher", SecurityUtils.isBlogPublisher());
			ctx.put("isBlogAdmin", SecurityUtils.isBlogAdmin());
			ctx.put(
					"isBlogAdminOrBlogOwner",
					SecurityUtils.isBlogAdmin()
							|| SecurityUtils.isUserAuthorisedForBlogAsBlogOwner((Blog) blog, SecurityUtils.getUsername()));
		}
		if (PebbleContext.getInstance().getConfiguration().isMultiBlog()) { // TODO duplicate of Blog check?
			ctx.put(Constants.BLOG_MANAGER, request.getAttribute(Constants.BLOG_MANAGER)); // TODO remove
			ctx.put(Constants.MULTI_BLOG_URL, request.getAttribute(Constants.MULTI_BLOG_URL));
			ctx.put(Constants.MULTI_BLOG_KEY, request.getAttribute(Constants.MULTI_BLOG_KEY));
		}
		ctx.put(Constants.RECENT_RESPONSES, request.getAttribute(Constants.RECENT_RESPONSES));
		ctx.put(Constants.RECENT_BLOG_ENTRIES, request.getAttribute(Constants.RECENT_BLOG_ENTRIES));
		for (String s : Arrays.asList("staticPages", "themes", "numbers", "countries", "languages", "timeZones",
				"characterEncodings")) {
			ctx.put(s, request.getAttribute(s));
		}

		// View properties
		ctx.put("title", request.getAttribute(Constants.TITLE_KEY));
		ctx.put("theme", request.getAttribute(Constants.THEME));
		ctx.put("themeHeadUri", request.getAttribute("themeHeadUri"));
		ctx.put("template", "template.vm");
		ctx.put("content", request.getAttribute("content"));


		Locale blogLocale = blog.getLocale();
		// General utilities
		ctx.put("fmt", new Formatter(blogLocale, blog.getTimeZone()));
		ctx.put("url", new UrlFunctions());
		// Calendar
		synchronized (days) {
			if (!days.containsKey(blogLocale)) {
				// convert zh_CN days to even shorter form
				String[] dayNames = new DateFormatSymbols(blog.getLocale()).getShortWeekdays();
				if (Locale.CHINA.equals(blogLocale) && dayNames[1].length() > 2) {
					for (int i = 1; i < dayNames.length; i++) {
						dayNames[i] = "周" + dayNames[i].substring(2);
					}
				}
				List<String> dn = new ArrayList<String>();
				for (String dayName : dayNames) {
					dn.add(dayName);
				}
				days.put(blogLocale, dn);
			}
		}
		ctx.put("days", days.get(blogLocale));
		ctx.put("calendarTool", new CalendarTag());
		ctx.put("yearNow", Calendar.getInstance().get(Calendar.YEAR));
		ctx.put("now", new Date());

		// Enumeration<?> atts = request.getAttributeNames();
		// while (atts.hasMoreElements()) {
		// String key = (String) atts.nextElement();
		// ctx.put(key, request.getAttribute(key));
		// }
		@SuppressWarnings("unchecked")
		Set<String> vmKeys = (Set<String>) request.getAttribute("vmkeys");
		if (vmKeys != null) {
			for (String key : vmKeys) {
				ctx.put(key, request.getAttribute(key));
			}
		}

		String name = FilenameUtils.getName(request.getRequestURI());
		return getTemplate(name);
	}

}
