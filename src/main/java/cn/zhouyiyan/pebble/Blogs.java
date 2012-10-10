package cn.zhouyiyan.pebble;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.domain.AbstractBlog;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.domain.BlogEntry;
import net.sourceforge.pebble.domain.BlogManager;
import net.sourceforge.pebble.domain.StaticPage;
import net.sourceforge.pebble.service.StaticPageService;
import net.sourceforge.pebble.service.StaticPageServiceException;
import net.sourceforge.pebble.util.Pageable;
import net.sourceforge.pebble.util.SecurityUtils;
import net.sourceforge.pebble.web.view.NotFoundView;
import net.sourceforge.pebble.web.view.RedirectView;
import net.sourceforge.pebble.web.view.View;
import net.sourceforge.pebble.web.view.impl.BlogEntriesView;
import net.sourceforge.pebble.web.view.impl.StaticPageView;

@Path("/")
public class Blogs {
	@Context
	HttpServletRequest request;

	@GET
	public View home() throws StaticPageServiceException {
		AbstractBlog abstractBlog = (AbstractBlog) request.getAttribute(Constants.BLOG_KEY);
		if (abstractBlog instanceof Blog) {
			Blog blog = (Blog) abstractBlog;
			String homePage = blog.getHomePage();
			if (homePage != null && !homePage.equals("")) { //
				return pages(homePage);
			}
		}
		return entriesByPage(1);
	}

	/**
	 * Views blog entries page by page. The page size is the same as the "number of blog entries shown on the home page".
	 */
	@GET
	@Path("/entries/{page}")
	public View entriesByPage(@PathParam("page") @DefaultValue("1") int page) {
		AbstractBlog abstractBlog = (AbstractBlog) request.getAttribute(Constants.BLOG_KEY);

		if (abstractBlog instanceof Blog) {
			Blog blog = (Blog) abstractBlog;
			boolean publishedOnly = !SecurityUtils.isUserAuthorisedForBlog(blog);

			request.setAttribute(Constants.MONTHLY_BLOG, blog.getBlogForThisMonth());
			request.setAttribute("displayMode", "page");
			request.setAttribute("page", page);

			Pageable<String> pageable;
			if (publishedOnly) {
				pageable = new Pageable<String>(blog.getBlogEntryIndex().getPublishedBlogEntries());
			} else {
				pageable = new Pageable<String>(blog.getBlogEntryIndex().getBlogEntries());
			}

			pageable.setPageSize(blog.getRecentBlogEntriesOnHomePage());
			pageable.setPage(page);
			List<String> blogEntryIds = pageable.getListForPage();
			List<BlogEntry> blogEntries = blog.getBlogEntries(blogEntryIds);

			request.setAttribute(Constants.BLOG_ENTRIES, blogEntries);
			request.setAttribute("pageable", pageable);

			return new BlogEntriesView();
		} else { // multiblog
			List<Blog> publicBlogs = BlogManager.getInstance().getPublicBlogs();
			if (publicBlogs.size() == 1) {
				Blog blog = publicBlogs.get(0);
				return new RedirectView(blog.getUrl());
			} else {
				request.setAttribute(Constants.BLOG_ENTRIES, abstractBlog.getRecentBlogEntries());
				return new BlogEntriesView();
			}
		}
	}

	/**
	 * Finds a particular static page by its name, ready to be displayed.
	 */
	@GET
	@Path("/pages/{name}")
	public View pages(@PathParam("name") String name) throws StaticPageServiceException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);

		StaticPageService service = new StaticPageService();
		StaticPage staticPage = service.getStaticPageByName(blog, name);
		if (staticPage == null) {
			// the page cannot be found - it may have been removed or the
			// requesting URL was wrong
			return new NotFoundView();
		}

		request.setAttribute(Constants.STATIC_PAGE_KEY, staticPage);
		request.setAttribute(Constants.MONTHLY_BLOG, blog.getBlogForThisMonth());
		request.setAttribute("displayMode", "detail");

		return new StaticPageView();
	}
}
