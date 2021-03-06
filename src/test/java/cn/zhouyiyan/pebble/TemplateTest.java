package cn.zhouyiyan.pebble;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.pebble.PebbleContext;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.domain.BlogEntry;
import net.sourceforge.pebble.domain.Category;
import net.sourceforge.pebble.domain.Day;
import net.sourceforge.pebble.domain.Month;
import net.sourceforge.pebble.domain.MultiBlog;
import net.sourceforge.pebble.domain.Response;
import net.sourceforge.pebble.domain.Tag;
import net.sourceforge.pebble.domain.Year;
import net.sourceforge.pebble.index.IndexedTag;
import net.sourceforge.pebble.security.PebbleUserDetails;
import net.sourceforge.pebble.web.tagext.CalendarTag;
import net.sourceforge.pebble.web.tagext.UrlFunctions;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Arrays;

public class TemplateTest {
	public static class ReadonlyRequest {
		private final HttpServletRequest request;

		public ReadonlyRequest(HttpServletRequest request) {
			this.request = request;
		}

		public Object getAttribute(String key) {
			return request.getAttribute(key);
		}

		public String getContextPath() {
			return request.getContextPath();
		}
	}

	@BeforeClass
	public static void setup() throws Exception {
		Properties props = new Properties();
		props.setProperty("file.resource.loader.path", "src/main/webapp/WEB-INF/templates,src/main/webapp/themes/default");
		Velocity.init(props);
	}

	@Test
	public void testLayout() throws Exception {
		Template template = Velocity.getTemplate("page.vm"); // page

		Date date = new Date();

		Day day = mock(Day.class);
		Month month = mock(Month.class);
		when(month.getBlogForDay(1)).thenReturn(day);
		when(month.before(month)).thenReturn(false);
		when(month.getPermalink()).thenReturn("http://zhouyiyan.cn/blog/month/0");
		when(month.getDate()).thenReturn(date);
		when(month.getLastDayInMonth()).thenReturn(1);
		when(month.getPreviousMonth()).thenReturn(month);
		when(month.getNextMonth()).thenReturn(month);
		when(month.getNumberOfBlogEntries()).thenReturn(1);
		when(day.getMonth()).thenReturn(month);
		when(day.getDate()).thenReturn(date);

		List<Month> months = new ArrayList<Month>();
		months.add(month);

		Year year = mock(Year.class);
		when(year.getDate()).thenReturn(date);
		when(year.getArchives()).thenReturn(months);

		List<Year> years = new ArrayList<Year>();
		years.add(year);

		IndexedTag tag = mock(IndexedTag.class);
		when(tag.getName()).thenReturn("tag1");
		when(tag.getRank()).thenReturn(2);
		when(tag.getPermalink()).thenReturn("http://zhouyiyan.cn/blog/tag/tag1");
		when(tag.getNumberOfBlogEntries()).thenReturn(1);

		List<Tag> tags = new ArrayList<Tag>();
		tags.add(tag);

		List<Category> categories = new ArrayList<Category>();
		Category category = mock(Category.class);
		when(category.getName()).thenReturn("Uncategorized");
		when(category.getPermalink()).thenReturn("http://zhouyiyan.cn/blog/cateogry/uncategorized");
		when(category.getSubCategories()).thenReturn(Collections.<Category> emptyList());
		categories.add(category);
		category = mock(Category.class);
		when(category.getName()).thenReturn("Life");
		when(category.getPermalink()).thenReturn("http://zhouyiyan.cn/blog/cateogry/life");
		when(category.getSubCategories()).thenReturn(Collections.<Category> emptyList());
		categories.add(category);

		Blog blog = mock(Blog.class);
		when(blog.getName()).thenReturn("My Blog");
		when(blog.getCharacterEncoding()).thenReturn("UTF-8");
		when(blog.getDescription()).thenReturn("some_description");
		when(blog.getAuthor()).thenReturn("Blog Owner");
		when(blog.getUrl()).thenReturn("http://zhouyiyan.cn/blog/");
		when(blog.getAbout()).thenReturn("About my blog");
		when(blog.getTimeZone()).thenReturn(TimeZone.getTimeZone("Asia/Shanghai"));
		when(blog.getLocale()).thenReturn(Locale.ENGLISH);
		when(blog.getBlogForToday()).thenReturn(day);
		when(blog.getCalendar()).thenReturn(Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"), Locale.ENGLISH));
		when(blog.getBlogForFirstMonth()).thenReturn(month);
		when(blog.getArchives()).thenReturn(years);
		when(blog.getCategories()).thenReturn(categories);
		when(blog.getTags()).thenReturn(tags);
		when(blog.getRecentResponsesOnHomePage()).thenReturn(1);

		List<Blog> blogs = new ArrayList<Blog>();
		blogs.add(blog);

		PebbleUserDetails pud = mock(PebbleUserDetails.class);
		when(pud.getProfile()).thenReturn("profile");
		when(pud.getUsername()).thenReturn("owner");

		BlogEntry blogEntry = mock(BlogEntry.class);
		when(blogEntry.getTagsAsCommaSeparated()).thenReturn("a,b,c");
		when(blogEntry.getUser()).thenReturn(pud);

		Response response = mock(Response.class);
		when(response.getTitle()).thenReturn("response title");
		when(response.getPermalink()).thenReturn("http://zhouyiyan.cn/blog/feedback/0");
		when(response.getTruncatedContent()).thenReturn("hello world");

		List<Response> responses = new ArrayList<Response>();
		responses.add(response);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getContextPath()).thenReturn("");

		MultiBlog multiBlog = mock(MultiBlog.class);
		when(multiBlog.getName()).thenReturn("MultiBlog");
		when(multiBlog.getUrl()).thenReturn("http://zhouyiyan.cn/blog/multi/");
		when(multiBlog.getDescription()).thenReturn("MultiDescription");

		VelocityContext context = new VelocityContext();
		context.put("fmt", new Formatter(blog.getLocale(), blog.getTimeZone()));
		context.put("url", new UrlFunctions());
		context.put("blogUrl", "http://zhouyiyan.cn/");
		context.put("blogType", "singleblog");
		context.put("title", "About");
		context.put("blog", blog);
		context.put("displayMode", "detail");
		context.put("blogEntry", blogEntry);
		context.put("theme", "deafult");
		context.put("request", new ReadonlyRequest(request));
		// context.put("themeHeadUri", "head.html");
		// context.put("isAuthenticated", "true");
		context.put("template", "template.vm");
		context.put("content", "blogEntries.html");
		context.put("days", Arrays.asList(new DateFormatSymbols(blog.getLocale()).getShortWeekdays()));
		context.put("calendarTool", new CalendarTag());
		context.put("archives", blog.getArchives());
		context.put("recentResponses", responses);
		context.put("blogs", blogs);
		context.put("multiBlog", multiBlog);
		context.put("pebbleContext", PebbleContext.getInstance());

		StringWriter sw = new StringWriter();
		template.merge(context, sw);

		System.out.println(sw.toString());
	}
}
