package cn.zhouyiyan.pebble;

import java.text.DateFormatSymbols;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.PebbleContext;
import net.sourceforge.pebble.domain.AbstractBlog;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.util.SecurityUtils;
import net.sourceforge.pebble.web.tagext.CalendarTag;
import net.sourceforge.pebble.web.tagext.UrlFunctions;

import org.apache.commons.io.FilenameUtils;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.servlet.VelocityServlet;

import cn.zhouyiyan.pebble.TemplateTest.Formatter;
import edu.emory.mathcs.backport.java.util.Arrays;

public class VelocityTemplateServlet extends VelocityServlet {
	private static final long serialVersionUID = 2560897251673033438L;

	@Override
	protected Template handleRequest(HttpServletRequest request, HttpServletResponse response, Context ctx)
			throws Exception {
		ctx.put("request", request);

		// Blog properties
		ctx.put("pebbleContext", PebbleContext.getInstance());
		AbstractBlog blog = (AbstractBlog) request.getAttribute(Constants.BLOG_KEY);
		ctx.put("blog", blog);
		ctx.put("blogUrl", blog.getUrl()); // TODO remove
		ctx.put("blogType", request.getAttribute(Constants.BLOG_TYPE));
		ctx.put("isAuthenticated", SecurityUtils.isUserAuthenticated());
		if (blog instanceof Blog) {
			ctx.put("archives", ((Blog) blog).getArchives());
		}
		ctx.put("recentResponses", request.getAttribute(Constants.RECENT_RESPONSES));

		// View properties
		ctx.put("title", request.getAttribute(Constants.TITLE_KEY));
		ctx.put("theme", request.getAttribute(Constants.THEME));
		ctx.put("themeHeadUri", request.getAttribute("themeHeadUri"));
		ctx.put("template", "template.vm");
		ctx.put("content", request.getAttribute("content"));

		// ctx.put("displayMode", "detail"); // TODO no where to inject
		// Blog entry
		ctx.put("blogEntry", request.getAttribute(Constants.BLOG_ENTRY_KEY));
		// Calendar
		ctx.put("days", Arrays.asList(new DateFormatSymbols(blog.getLocale()).getShortWeekdays()));
		ctx.put("calendarTool", new CalendarTag());
		// Blog list
		ctx.put("blogs", request.getAttribute(Constants.BLOGS));
		// MultiBlog
		ctx.put("multiBlog", request.getAttribute(Constants.MULTI_BLOG_KEY));

		// General utilities
		ctx.put("fmt", new Formatter(blog.getLocale(), blog.getTimeZone()));
		ctx.put("url", new UrlFunctions());

		String name = FilenameUtils.getName(request.getRequestURI());
		return getTemplate(name);
	}

}
