package cn.zhouyiyan.pebble;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.PebbleContext;
import net.sourceforge.pebble.domain.AbstractBlog;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.domain.BlogEntry;
import net.sourceforge.pebble.domain.BlogManager;
import net.sourceforge.pebble.domain.StaticPage;
import net.sourceforge.pebble.security.PebbleUserDetails;
import net.sourceforge.pebble.security.SecurityRealmException;
import net.sourceforge.pebble.service.StaticPageService;
import net.sourceforge.pebble.service.StaticPageServiceException;
import net.sourceforge.pebble.util.Pageable;
import net.sourceforge.pebble.util.SecurityUtils;
import net.sourceforge.pebble.web.view.NotFoundView;
import net.sourceforge.pebble.web.view.RedirectView;
import net.sourceforge.pebble.web.view.View;
import net.sourceforge.pebble.web.view.impl.BlogEntriesView;
import net.sourceforge.pebble.web.view.impl.BlogPropertiesView;
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
	 * Edits the properties associated with the current Blog.
	 */
	@GET
	@Path("/customizations")
	public View customizations() throws SecurityRealmException, StaticPageServiceException {
		checkUserInRoles(Constants.BLOG_ADMIN_ROLE, Constants.BLOG_OWNER_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);

		setAttribute("properties", blog.getProperties());

		Set<?> resources = request.getSession().getServletContext().getResourcePaths("/themes/");
		List<String> themes = new ArrayList<String>();
		Iterator<?> it = resources.iterator();
		String resource;
		while (it.hasNext()) {
			resource = (String) it.next();
			resource = resource.substring(8, resource.length() - 1);
			if (!resource.startsWith("_")) {
				themes.add(resource);
			}
		}
		setAttribute("themes", themes);

		List<Integer> numbers = new ArrayList<Integer>();
		for (int i = 0; i <= 20; i++) {
			numbers.add(i);
		}
		setAttribute("numbers", numbers);

		setAttribute("countries", Locale.getISOCountries());
		setAttribute("languages", Locale.getISOLanguages());
		List<String> timeZones = Arrays.asList(java.util.TimeZone.getAvailableIDs());
		Collections.sort(timeZones);
		setAttribute("timeZones", timeZones);
		setAttribute("characterEncodings", Charset.availableCharsets().keySet());
		setAttribute(
				"blogOwnerUsers",
				filterUsersByRole(PebbleContext.getInstance().getConfiguration().getSecurityRealm().getUsers(),
						Constants.BLOG_OWNER_ROLE));
		setAttribute(
				"blogPublisherUsers",
				filterUsersByRole(PebbleContext.getInstance().getConfiguration().getSecurityRealm().getUsers(),
						Constants.BLOG_PUBLISHER_ROLE));
		setAttribute(
				"blogContributorUsers",
				filterUsersByRole(PebbleContext.getInstance().getConfiguration().getSecurityRealm().getUsers(),
						Constants.BLOG_CONTRIBUTOR_ROLE));
		setAttribute("allUsers", PebbleContext.getInstance().getConfiguration().getSecurityRealm().getUsers());

		StaticPageService service = new StaticPageService();
		List<StaticPage> staticPages = service.getStaticPages(blog);
		StaticPage defaultPage = new StaticPage(blog);
		defaultPage.setName("");
		defaultPage.setTitle("Default - recent blog entries");
		staticPages.add(0, defaultPage);
		setAttribute("staticPages", staticPages);

		return new BlogPropertiesView();
	}

	private void setAttribute(String name, Object o) {
		request.setAttribute(name, o);
	}

	private List<PebbleUserDetails> filterUsersByRole(Collection<PebbleUserDetails> users, String role) {
		List<PebbleUserDetails> list = new LinkedList<PebbleUserDetails>();
		for (PebbleUserDetails user : users) {
			if (user.isUserInRole(role)) {
				list.add(user);
			}
		}

		return list;
	}

	private void checkUserInRoles(String... roles) {
		AbstractBlog ab = (AbstractBlog) request.getAttribute(Constants.BLOG_KEY);
		String currentUser = SecurityUtils.getUsername();
		for (String role : roles) {
			if (role.equals(Constants.ANY_ROLE)) {
				return;
			} else if (SecurityUtils.isUserInRole(role)) {
				if (ab instanceof Blog) {
					Blog blog = (Blog) ab;
					if (blog.isUserInRole(role, currentUser)) return;
				} else {
					return;
				}
			}
		}
		throw new WebApplicationException(Status.FORBIDDEN);
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

			setAttribute(Constants.MONTHLY_BLOG, blog.getBlogForThisMonth());
			setAttribute("displayMode", "page");
			setAttribute("page", page);

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

			setAttribute(Constants.BLOG_ENTRIES, blogEntries);
			setAttribute("pageable", pageable);

			return new BlogEntriesView();
		} else { // multiblog
			List<Blog> publicBlogs = BlogManager.getInstance().getPublicBlogs();
			if (publicBlogs.size() == 1) {
				Blog blog = publicBlogs.get(0);
				return new RedirectView(blog.getUrl());
			} else {
				setAttribute(Constants.BLOG_ENTRIES, abstractBlog.getRecentBlogEntries());
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

		setAttribute(Constants.STATIC_PAGE_KEY, staticPage);
		setAttribute(Constants.MONTHLY_BLOG, blog.getBlogForThisMonth());
		setAttribute("displayMode", "detail");

		return new StaticPageView();
	}
}
