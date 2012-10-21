package cn.zhouyiyan.pebble;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.PebbleContext;
import net.sourceforge.pebble.api.confirmation.CommentConfirmationStrategy;
import net.sourceforge.pebble.api.decorator.ContentDecoratorContext;
import net.sourceforge.pebble.comparator.BlogEntryComparator;
import net.sourceforge.pebble.comparator.PageBasedContentByTitleComparator;
import net.sourceforge.pebble.dao.CategoryDAO;
import net.sourceforge.pebble.dao.DAOFactory;
import net.sourceforge.pebble.dao.PersistenceException;
import net.sourceforge.pebble.domain.AbstractBlog;
import net.sourceforge.pebble.domain.Attachment;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.domain.BlogEntry;
import net.sourceforge.pebble.domain.BlogManager;
import net.sourceforge.pebble.domain.BlogService;
import net.sourceforge.pebble.domain.BlogServiceException;
import net.sourceforge.pebble.domain.Category;
import net.sourceforge.pebble.domain.Comment;
import net.sourceforge.pebble.domain.Day;
import net.sourceforge.pebble.domain.Month;
import net.sourceforge.pebble.domain.Response;
import net.sourceforge.pebble.domain.StaticPage;
import net.sourceforge.pebble.domain.Tag;
import net.sourceforge.pebble.search.SearchException;
import net.sourceforge.pebble.search.SearchHit;
import net.sourceforge.pebble.search.SearchResults;
import net.sourceforge.pebble.security.PebbleUserDetails;
import net.sourceforge.pebble.security.SecurityRealmException;
import net.sourceforge.pebble.service.DefaultLastModifiedService;
import net.sourceforge.pebble.service.LastModifiedService;
import net.sourceforge.pebble.service.StaticPageService;
import net.sourceforge.pebble.service.StaticPageServiceException;
import net.sourceforge.pebble.util.CookieUtils;
import net.sourceforge.pebble.util.I18n;
import net.sourceforge.pebble.util.MailUtils;
import net.sourceforge.pebble.util.Pageable;
import net.sourceforge.pebble.util.SecurityUtils;
import net.sourceforge.pebble.util.StringUtils;
import net.sourceforge.pebble.web.validation.ValidationContext;
import net.sourceforge.pebble.web.view.NotFoundView;
import net.sourceforge.pebble.web.view.NotModifiedView;
import net.sourceforge.pebble.web.view.RedirectView;
import net.sourceforge.pebble.web.view.View;
import net.sourceforge.pebble.web.view.impl.AbstractRomeFeedView;
import net.sourceforge.pebble.web.view.impl.AdvancedSearchView;
import net.sourceforge.pebble.web.view.impl.BlogEntriesByDayView;
import net.sourceforge.pebble.web.view.impl.BlogEntriesByMonthView;
import net.sourceforge.pebble.web.view.impl.BlogEntriesView;
import net.sourceforge.pebble.web.view.impl.BlogEntryFormView;
import net.sourceforge.pebble.web.view.impl.BlogEntryView;
import net.sourceforge.pebble.web.view.impl.BlogPropertiesView;
import net.sourceforge.pebble.web.view.impl.BlogSecurityView;
import net.sourceforge.pebble.web.view.impl.CategoriesView;
import net.sourceforge.pebble.web.view.impl.CommentConfirmationView;
import net.sourceforge.pebble.web.view.impl.CommentFormView;
import net.sourceforge.pebble.web.view.impl.ConfirmCommentView;
import net.sourceforge.pebble.web.view.impl.FeedView;
import net.sourceforge.pebble.web.view.impl.LoginPageView;
import net.sourceforge.pebble.web.view.impl.PublishBlogEntryView;
import net.sourceforge.pebble.web.view.impl.RdfView;
import net.sourceforge.pebble.web.view.impl.ResponsesView;
import net.sourceforge.pebble.web.view.impl.SearchResultsView;
import net.sourceforge.pebble.web.view.impl.StaticPageFormView;
import net.sourceforge.pebble.web.view.impl.StaticPageLockedView;
import net.sourceforge.pebble.web.view.impl.StaticPageView;
import net.sourceforge.pebble.web.view.impl.StaticPagesView;
import net.sourceforge.pebble.web.view.impl.SubscribeView;
import net.sourceforge.pebble.web.view.impl.SubscribedView;
import net.sourceforge.pebble.web.view.impl.UnpublishedBlogEntriesView;
import net.sourceforge.pebble.web.view.impl.UserView;
import net.sourceforge.pebble.web.view.impl.UsersView;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class Blogs {
	private static final Logger LOG = LoggerFactory.getLogger(Blogs.class);
	@Context
	HttpServletRequest request;

	@Context
	HttpServletResponse response;

	LastModifiedService lastModifiedService = new DefaultLastModifiedService();

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
		return recentEntries(1);
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
		setAttribute(request, name, o);
	}

	static void setAttribute(HttpServletRequest request, String name, Object o) {
		if (o == null) return;
		@SuppressWarnings("unchecked")
		Set<String> vmKeys = (Set<String>) request.getAttribute("vmkeys");
		if (vmKeys == null) {
			vmKeys = new HashSet<String>();
			request.setAttribute("vmkeys", vmKeys);
		}
		vmKeys.add(name);
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

	boolean isSecured = true;

	private void checkUserInRoles(String... roles) {
		if (!isSecured) return;
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
	 * Edits the security properties associated with the current Blog.
	 */
	@GET
	@Path("/security")
	public View security() throws SecurityRealmException {
		checkUserInRoles(Constants.BLOG_ADMIN_ROLE, Constants.BLOG_OWNER_ROLE);
		Collection<PebbleUserDetails> users = PebbleContext.getInstance().getConfiguration().getSecurityRealm().getUsers();
		setAttribute("blogOwnerUsers", filterUsersByRole(users, Constants.BLOG_OWNER_ROLE));
		setAttribute("blogPublisherUsers", filterUsersByRole(users, Constants.BLOG_PUBLISHER_ROLE));
		setAttribute("blogContributorUsers", filterUsersByRole(users, Constants.BLOG_CONTRIBUTOR_ROLE));
		setAttribute("allUsers", users);
    return new BlogSecurityView();
	}
	
	/**
	 * Adds a new blog entry. This is called to create a blank blog entry to
	 * populate a HTML form containing the contents of that entry.
	 */
	@GET
	@Path("/entries/add")
	public View addEntry(@QueryParam("entryToClone") String entryToClone) throws BlogServiceException {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		BlogEntry blogEntryToClone = null;

		if (entryToClone != null && entryToClone.length() > 0) {
			BlogService service = new BlogService();
			blogEntryToClone = service.getBlogEntry(blog, entryToClone);
		}

		BlogEntry entry = new BlogEntry(blog);
		if (blogEntryToClone != null) {
			entry.setTitle(blogEntryToClone.getTitle());
			entry.setSubtitle(blogEntryToClone.getSubtitle());
			entry.setBody(blogEntryToClone.getBody());
			entry.setExcerpt(blogEntryToClone.getExcerpt());
			entry.setTimeZoneId(blogEntryToClone.getTimeZoneId());
			entry.setCommentsEnabled(blogEntryToClone.isCommentsEnabled());
			entry.setTrackBacksEnabled(blogEntryToClone.isTrackBacksEnabled());

			// copy the categories
			Iterator<Category> it = blogEntryToClone.getCategories().iterator();
			while (it.hasNext()) {
				entry.addCategory(it.next());
			}

			entry.setTags(blogEntryToClone.getTags());
		} else {
			entry.setTitle("Title");
			entry.setBody("<p>\n\n</p>");
		}

		entry.setAuthor(SecurityUtils.getUsername());

		setAttribute(Constants.BLOG_ENTRY_KEY, entry);

		return new BlogEntryFormView();
	}

	public View cloneEntry(String entryToClone) throws BlogServiceException {
		return addEntry(entryToClone);
	}

	/**
	 * Edits an existing blog entry. This is called to populate a HTML form
	 * containing the contents of the blog entry.
	 */
	@GET
	@Path("/entries/edit/{entryId:\\d+}")
	public View editEntry(@PathParam("entryId") String entryId) throws BlogServiceException {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		BlogEntry blogEntry = new BlogService().getBlogEntry(blog, entryId);

		if (blogEntry == null) {
			return new NotFoundView();
		} else {
			setAttribute(Constants.BLOG_ENTRY_KEY, blogEntry);
			return new BlogEntryFormView();
		}
	}

	/**
	 * Finds a particular blog entry, ready to be displayed.
	 */
	@GET
	@Path("/entries/{entryId:\\d+}")
	public View getEntry(@PathParam("entryId") String entryId) throws BlogServiceException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		BlogEntry blogEntry = new BlogService().getBlogEntry(blog, entryId);

		if (blogEntry == null) {
			// the entry cannot be found - it may have been removed or the
			// requesting URL was wrong

			return new NotFoundView();
		} else if (!blogEntry.isPublished() && !(SecurityUtils.isUserAuthorisedForBlog(blog))) {
			// the entry exists, but isn't yet published
			return new NotFoundView();
		} else {
			setAttribute(Constants.BLOG_ENTRY_KEY, blogEntry);
			setAttribute(Constants.MONTHLY_BLOG, blog.getBlogForDay(blogEntry.getDate()).getMonth());
			setAttribute("displayMode", "detail");

			// is "remember me" set?
			Cookie rememberMe = CookieUtils.getCookie(request.getCookies(), "rememberMe");
			if (rememberMe != null) {
				setAttribute("rememberMe", "true");
			}

			ContentDecoratorContext decoratorContext = new ContentDecoratorContext();
			decoratorContext.setView(ContentDecoratorContext.DETAIL_VIEW);
			decoratorContext.setMedia(ContentDecoratorContext.HTML_PAGE);
			Comment comment = createBlankComment(blog, blogEntry, request);
			Comment decoratedComment = (Comment) comment.clone();
			blog.getContentDecoratorChain().decorate(decoratorContext, decoratedComment);
			setAttribute("decoratedComment", decoratedComment);
			setAttribute("undecoratedComment", comment);

			return new BlogEntryView();
		}
	}

	private Comment createBlankComment(Blog blog, BlogEntry blogEntry, HttpServletRequest request) {
		Comment comment = blogEntry.createComment("", "", "", "", "", "", request.getRemoteAddr());

		// populate the author, email and website from one of :
		// - the logged in user details
		// - the "remember me" cookie
		if (SecurityUtils.isUserAuthenticated()) {
			PebbleUserDetails user = SecurityUtils.getUserDetails();
			if (user != null) {
				comment.setAuthor(user.getName());
				comment.setEmail(user.getEmailAddress());
				if (user.getWebsite() != null && !user.getWebsite().equals("")) {
					comment.setWebsite(user.getWebsite());
				} else {
					comment.setWebsite(blogEntry.getBlog().getUrl() + "authors/" + user.getUsername() + "/");
				}
				comment.setAuthenticated(true);
			}
		} else {
			try {
				// is "remember me" set?
				Cookie rememberMe = CookieUtils.getCookie(request.getCookies(), "rememberMe");
				if (rememberMe != null) {
					// remember me has been checked and we're not already previewing a
					// comment
					// so create a new comment as this will populate the
					// author/email/website
					Cookie author = CookieUtils.getCookie(request.getCookies(), "rememberMe.author");
					if (author != null) {
						comment.setAuthor(URLDecoder.decode(author.getValue(), blog.getCharacterEncoding()));
					}

					Cookie email = CookieUtils.getCookie(request.getCookies(), "rememberMe.email");
					if (email != null) {
						comment.setEmail(URLDecoder.decode(email.getValue(), blog.getCharacterEncoding()));
					}

					Cookie website = CookieUtils.getCookie(request.getCookies(), "rememberMe.website");
					if (website != null) {
						comment.setWebsite(URLDecoder.decode(website.getValue(), blog.getCharacterEncoding()));
					}
				}
			} catch (UnsupportedEncodingException e) {
				LOG.error("Exception encountered", e);
			}
		}

		// are we replying to an existing comment?
		String parentCommentId = request.getParameter("comment");
		if (parentCommentId != null && parentCommentId.length() > 0) {
			long parent = Long.parseLong(parentCommentId);
			Comment parentComment = blogEntry.getComment(parent);
			if (parentComment != null) {
				comment.setParent(parentComment);
				comment.setTitle(parentComment.getTitle());
			}
		}

		return comment;
	}

	/**
	 * Views blog entries page by page. The page size is the same as the
	 * "number of blog entries shown on the home page".
	 */
	@GET
	@Path("/entries/recent")
	public View recentEntries(@QueryParam("page") @DefaultValue("1") int page) {
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
	 * Allows the user to view the unpublised blog entries associated with the
	 * current blog.
	 */
	@GET
	@Path("/entries/unpublished")
	public View unpublishedEntries() {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		List<BlogEntry> blogEntries = blog.getUnpublishedBlogEntries();
		Collections.sort(blogEntries, new PageBasedContentByTitleComparator());
		setAttribute("unpublishedBlogEntries", blogEntries);
		return new UnpublishedBlogEntriesView();
	}

	/**
	 * Allows the user to manage (currently only remove) one or more blog entries.
	 */
	@POST
	@Path("/entries/manage")
	public View manageEntries(@FormParam("submit") String submit, //
			@FormParam("entry") String[] ids, //
			@FormParam("redirectUrl") String redirectUrl) throws BlogServiceException {
		if ("Remove".equalsIgnoreCase(submit)) {
			checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		} else if ("Publish".equalsIgnoreCase(submit)) {
			checkUserInRoles(Constants.BLOG_PUBLISHER_ROLE);
		} else {
			checkUserInRoles(Constants.BLOG_OWNER_ROLE);
		}
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);

		if (ids != null) {
			for (String id : ids) {
				BlogService service = new BlogService();
				BlogEntry blogEntry = service.getBlogEntry(blog, id);

				if (blogEntry != null) {
					if ("Remove".equalsIgnoreCase(submit)) {
						service.removeBlogEntry(blogEntry);
						blog.info("Blog entry \"" + StringUtils.transformHTML(blogEntry.getTitle()) + "\" removed.");
					} else if ("Publish".equals(submit)) {
						// this publishes the entry as-is (i.e. with the same
						// date/time it already has)
						blogEntry.setPublished(true);
						service.putBlogEntry(blogEntry);
						blog.info("Blog entry <a href=\"" + blogEntry.getLocalPermalink() + "\">" + blogEntry.getTitle()
								+ "</a> published.");
					}
				}
			}
		}

		if (redirectUrl != null && redirectUrl.trim().length() > 0) {
			return new RedirectView(redirectUrl);
		} else {
			return new RedirectView(blog.getUrl());
		}
	}

	/**
	 * Allows the user to manage (edit, remove, etc) a blog entry.
	 */
	@POST
	@Path("/entries/manage/{entryId:\\d+}")
	public View manageEntry(@PathParam("entryId") String id, //
			@FormParam("submit") String submit, //
			@FormParam("confirm") String confirm) throws BlogServiceException, StaticPageServiceException {
		if ("Publish".equalsIgnoreCase(submit) || "Unpublish".equalsIgnoreCase(submit)) {
			checkUserInRoles(Constants.BLOG_PUBLISHER_ROLE);
		} else if ("Remove".equalsIgnoreCase(submit) || "Edit".equalsIgnoreCase(submit)) {
			checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		} else {
			checkUserInRoles(Constants.BLOG_OWNER_ROLE);
		}

		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);

		BlogService service = new BlogService();
		BlogEntry blogEntry = service.getBlogEntry(blog, id);

		if (blogEntry == null) {
			return new NotFoundView();
		} else if ("Edit".equals(submit)) {
			// return new ForwardView("/p/entries/edit/" + id);
			return editEntry(id);
		} else if ("Publish".equals(submit) || "Unpublish".equals(submit)) {
			setAttribute(Constants.BLOG_ENTRY_KEY, blogEntry);
			return new PublishBlogEntryView();
		} else if ("Clone".equals(submit)) {
			// return new ForwardView("/p/entries/add?entryToClone=" +
			// blogEntry.getId());
			return cloneEntry(blogEntry.getId());
		} else if ("true".equals(confirm)) {
			if (submit.equalsIgnoreCase("Remove")) {
				service.removeBlogEntry(blogEntry);
				blog.info("Blog entry \"" + StringUtils.transformHTML(blogEntry.getTitle()) + "\" removed.");
				// return new ForwardView("/p"); // home
				return home();
			}
		}

		return new RedirectView(blogEntry.getLocalPermalink());
	}

	/**
	 * Allows the user to publish/unpublish a blog entry.
	 */
	@POST
	@Path("/entries/publish/{entryId:\\d+}")
	public View publishEntry(@PathParam("entryId") String id, //
			@FormParam("submit") String submit, //
			@FormParam("publishDate") String publishDate) throws BlogServiceException {
		checkUserInRoles(Constants.BLOG_PUBLISHER_ROLE);

		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		BlogService service = new BlogService();
		BlogEntry blogEntry = service.getBlogEntry(blog, id);

		if (blogEntry == null) {
			return new NotFoundView();
		} else if ("Publish".equals(submit)) {
			DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, blog.getLocale());
			dateFormat.setTimeZone(blog.getTimeZone());
			dateFormat.setLenient(false);

			if ("as-is".equalsIgnoreCase(publishDate)) {
				// this is the easiest scenario - just set the blog entry to published
				// TODO: localization
				try {
					blogEntry.setPublished(true);
					service.putBlogEntry(blogEntry);
					blog.info("Blog entry <a href=\"" + blogEntry.getLocalPermalink() + "\">" + blogEntry.getTitle()
							+ "</a> published.");
				} catch (BlogServiceException be) {
					// give feedback to the user that something bad has happened
					blog.error("Error publishing blog entry " + StringUtils.transformHTML(blogEntry.getTitle()) + ": "
							+ be.getClass().getName() + " " + StringUtils.transformHTML(be.getMessage()));
					LOG.error("", be);
				}
			} else {
				Date date = new Date();
				if ("custom".equalsIgnoreCase(publishDate)) {
					Date now = new Date();
					String dateAsString = request.getParameter("date");
					if (dateAsString != null && dateAsString.length() > 0) {
						try {
							date = dateFormat.parse(dateAsString);
							if (date.after(now)) {
								date = now;
							}
						} catch (ParseException pe) {
							LOG.warn("", pe);
						}
					}
				}

				// now save the published entry and remove the unpublished version
				try {
					LOG.info("Removing blog entry dated {}", blogEntry.getDate());
					service.removeBlogEntry(blogEntry);

					blogEntry.setDate(date);
					blogEntry.setPublished(true);
					LOG.info("Putting blog entry dated {}", blogEntry.getDate());
					service.putBlogEntry(blogEntry);
					blog.info("Blog entry <a href=\"" + blogEntry.getLocalPermalink() + "\">" + blogEntry.getTitle()
							+ "</a> published.");
				} catch (BlogServiceException be) {
					LOG.error("", be);
				}
			}
		} else if ("Unpublish".equals(submit)) {
			blogEntry.setPublished(false);
			try {
				service.putBlogEntry(blogEntry);
				blog.info("Blog entry <a href=\"" + blogEntry.getLocalPermalink() + "\">" + blogEntry.getTitle()
						+ "</a> unpublished.");
			} catch (BlogServiceException be) {
				LOG.error("", be);
			}
		}

		return new RedirectView(blogEntry.getLocalPermalink());
	}

	/** the value used if the blog entry is being previewed rather than added */
	private static final String PREVIEW = "Preview";

	/**
	 * Saves a blog entry.
	 */
	@POST
	@Path("entries/save/{entryId:\\d+}")
	public View saveEntry(@PathParam("entryId") String id, //
			@FormParam("submit") String submitType, @FormParam("persistent") String persistent) throws BlogServiceException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		if (submitType != null && submitType.equalsIgnoreCase(PREVIEW)) {
			return previewBlogEntry(blog, id, persistent);
		} else {
			return saveBlogEntry(blog, id, persistent);
		}
	}

	private View previewBlogEntry(Blog blog, String id, String persistent) throws BlogServiceException {
		BlogEntry blogEntry = getBlogEntry(blog, id, persistent);

		populateBlogEntry(blogEntry, request);

		ValidationContext validationContext = new ValidationContext();
		blogEntry.validate(validationContext);
		setAttribute("validationContext", validationContext);
		setAttribute(Constants.BLOG_ENTRY_KEY, blogEntry);

		return new BlogEntryFormView();
	}

	private View saveBlogEntry(Blog blog, String id, String persistent) throws BlogServiceException {
		BlogEntry blogEntry = getBlogEntry(blog, id, persistent);

		populateBlogEntry(blogEntry, request);

		ValidationContext context = new ValidationContext();
		blogEntry.validate(context);

		setAttribute("validationContext", context);
		setAttribute(Constants.BLOG_ENTRY_KEY, blogEntry);

		if (context.hasErrors()) {
			return new BlogEntryFormView();
		} else {
			BlogService service = new BlogService();
			try {
				service.putBlogEntry(blogEntry);
				blog.info("Blog entry <a href=\"" + blogEntry.getLocalPermalink() + "\">" + blogEntry.getTitle()
						+ "</a> saved.");
				setAttribute(Constants.BLOG_ENTRY_KEY, blogEntry);
				return new RedirectView(blogEntry.getLocalPermalink());
			} catch (BlogServiceException be) {
				LOG.error(be.getMessage(), be);
				context.addError(be.getMessage());
				be.printStackTrace();
				return new BlogEntryFormView();
			}
		}
	}

	private BlogEntry getBlogEntry(Blog blog, String id, String persistent) throws BlogServiceException {
		if ("true".equalsIgnoreCase(persistent)) {
			return new BlogService().getBlogEntry(blog, id);
		} else {
			BlogEntry blogEntry = new BlogEntry(blog);
			blogEntry.setAuthor(SecurityUtils.getUsername());
			return blogEntry;
		}
	}

	private void populateBlogEntry(BlogEntry blogEntry, HttpServletRequest request) {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		String title = StringUtils.stripScriptTags(request.getParameter("title"));
		String subtitle = StringUtils.stripScriptTags(request.getParameter("subtitle"));
		String body = StringUtils.filterNewlines(request.getParameter("body"));
		String excerpt = StringUtils.filterNewlines(request.getParameter("excerpt"));
		String originalPermalink = request.getParameter("originalPermalink");
		String tags = request.getParameter("tags");
		String commentsEnabled = request.getParameter("commentsEnabled");
		String trackBacksEnabled = request.getParameter("trackBacksEnabled");
		String category[] = request.getParameterValues("category");
		String timeZone = request.getParameter("timeZone");

		// the date can only set on those entries that have not yet been persisted
		if (!blogEntry.isPersistent()) {
			DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, blog.getLocale());
			dateFormat.setTimeZone(blog.getTimeZone());
			dateFormat.setLenient(false);

			Date now = new Date();
			String dateAsString = request.getParameter("date");
			if (dateAsString != null && dateAsString.length() > 0) {
				try {
					Date date = dateFormat.parse(dateAsString);
					if (date.after(now)) {
						date = now;
					}
					blogEntry.setDate(date);
				} catch (ParseException pe) {
					LOG.warn("", pe);
					blogEntry.setDate(now);
				}
			} else {
				// the date has been blanked out, so reset to "now"
				blogEntry.setDate(now);
			}
		}

		blogEntry.setTimeZoneId(timeZone);
		blogEntry.setTitle(title);
		blogEntry.setSubtitle(subtitle);
		blogEntry.setBody(body);
		blogEntry.setExcerpt(excerpt);
		Set<Category> categories = new HashSet<Category>();
		if (category != null) {
			for (int i = 0; i < category.length; i++) {
				categories.add(blog.getCategory(category[i]));
			}
		}
		blogEntry.setCategories(categories);
		blogEntry.setTags(tags);
		blogEntry.setOriginalPermalink(originalPermalink);
		if (commentsEnabled != null && commentsEnabled.equalsIgnoreCase("true")) {
			blogEntry.setCommentsEnabled(true);
		} else {
			blogEntry.setCommentsEnabled(false);
		}
		if (trackBacksEnabled != null && trackBacksEnabled.equalsIgnoreCase("true")) {
			blogEntry.setTrackBacksEnabled(true);
		} else {
			blogEntry.setTrackBacksEnabled(false);
		}

		String attachmentUrl = request.getParameter("attachmentUrl");
		String attachmentSize = request.getParameter("attachmentSize");
		String attachmentType = request.getParameter("attachmentType");
		if (attachmentUrl != null && attachmentUrl.length() > 0) {
			Attachment attachment = populateAttachment(blogEntry, attachmentUrl, attachmentSize, attachmentType);
			blogEntry.setAttachment(attachment);
		} else {
			blogEntry.setAttachment(null);
		}
	}

	private Attachment populateAttachment(BlogEntry blogEntry, String attachmentUrl, String attachmentSize,
			String attachmentType) {
		if (attachmentSize == null || attachmentSize.length() == 0) {
			String absoluteAttachmentUrl = attachmentUrl;
			try {
				HttpClient httpClient = new HttpClient();
				if (absoluteAttachmentUrl.startsWith("./")) {
					absoluteAttachmentUrl = blogEntry.getBlog().getUrl() + absoluteAttachmentUrl.substring(2);
				}

				HeadMethod headMethod = new HeadMethod(absoluteAttachmentUrl);
				int status = httpClient.executeMethod(headMethod);
				if (status == 200) {
					Header attachmentSizeHeader = headMethod.getResponseHeader("Content-Length");
					if (attachmentSizeHeader != null) {
						attachmentSize = attachmentSizeHeader.getValue();
					}
					Header attachmentTypeHeader = headMethod.getResponseHeader("Content-Type");
					if (attachmentTypeHeader != null) {
						attachmentType = attachmentTypeHeader.getValue();
					}
				}
			} catch (IOException e) {
				LOG.warn("Could not get details for attachment located at " + absoluteAttachmentUrl + " : " + e.getMessage());
			}
		}

		Attachment attachment = new Attachment();
		attachment.setUrl(attachmentUrl);
		if (attachmentSize != null && attachmentSize.length() > 0) {
			attachment.setSize(Long.parseLong(attachmentSize));
		}
		attachment.setType(attachmentType);

		return attachment;
	}

	/**
	 * Finds all blog entries for a particular month, ready for them to be displayed.
	 */
	@GET
	@Path("entries/{year:\\d+}/{month:\\d+}")
	public View recentEntriesByMonth(@PathParam("year") int year, @PathParam("month") int month)
			throws BlogServiceException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		Month monthly = blog.getBlogForMonth(year, month);
		List<BlogEntry> blogEntries = new BlogService().getBlogEntries(blog, year, month);
		setAttribute(Constants.BLOG_ENTRIES, filter(blog, blogEntries, request));
		setAttribute("displayMode", "month");
		setAttribute(Constants.MONTHLY_BLOG, monthly);

		// put the previous and next months in the model for navigation purposes
		Month firstMonth = blog.getBlogForFirstMonth();
		Month previousMonth = monthly.getPreviousMonth();
		Month nextMonth = monthly.getNextMonth();

		if (!previousMonth.before(firstMonth)) {
			setAttribute("previousMonth", previousMonth);
		}

		if (!nextMonth.getDate().after(blog.getCalendar().getTime()) || nextMonth.before(firstMonth)) {
			setAttribute("nextMonth", nextMonth);
		}

		return new BlogEntriesByMonthView();
	}

	private List<BlogEntry> filter(Blog blog, List<BlogEntry> blogEntries, HttpServletRequest request) {
		List<BlogEntry> filtered = new ArrayList<BlogEntry>();

		for (BlogEntry blogEntry : blogEntries) {
			if (blogEntry.isPublished() || ((SecurityUtils.isUserAuthorisedForBlog(blog) && blogEntry.isUnpublished()))) {
				filtered.add(blogEntry);
			}
		}

		return filtered;
	}

	/**
	 * Finds all blog entries for a particular day, ready for them to be displayed.
	 */
	@GET
	@Path("/entries/{year:\\d+}/{month:\\d+}/{day:\\d+}")
	public View recentEntriesByDay(@PathParam("year") int year, @PathParam("month") int month, @PathParam("day") int day)
			throws BlogServiceException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);

		Day daily = year == 0 && month == 0 && day == 0 ? blog.getBlogForToday() : blog.getBlogForDay(year, month, day);

		List<BlogEntry> blogEntries = new BlogService().getBlogEntries(blog, year, month, day);

		setAttribute(Constants.MONTHLY_BLOG, daily.getMonth());
		setAttribute(Constants.DAILY_BLOG, daily);
		setAttribute(Constants.BLOG_ENTRIES, filter(blog, blogEntries, request));
		setAttribute("displayMode", "day");

		// put the previous and next days in the model for navigation purposes
		Day firstDay = blog.getBlogForFirstMonth().getBlogForFirstDay();
		Day previousDay = daily.getPreviousDay();
		Day nextDay = daily.getNextDay();

		if (!previousDay.before(firstDay)) {
			setAttribute("previousDay", previousDay);
		}

		if (!nextDay.getDate().after(blog.getCalendar().getTime()) || nextDay.before(firstDay)) {
			setAttribute("nextDay", nextDay);
		}

		return new BlogEntriesByDayView();
	}

	/**
	 * Allows the user to reply to a specific blog entry.
	 */
	@GET
	@Path("/comments/reply/{entryId}")
	public View replyTo(@PathParam("entryId") String entryId, //
			@QueryParam("comment") String parentCommentId) throws BlogServiceException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);

		BlogService service = new BlogService();
		BlogEntry blogEntry = null;
		if (entryId != null) {
			blogEntry = service.getBlogEntry(blog, entryId);
		}

		if (blogEntry == null) {
			// the entry cannot be found - it may have been removed or the requesting
			// URL was wrong

			return new NotFoundView();
		} else if (!blogEntry.isPublished() && !(SecurityUtils.isUserAuthorisedForBlog(blog))) {
			// the entry exists, but isn't yet published
			return new NotFoundView();
		} else if (!blogEntry.isCommentsEnabled()) {
			setAttribute(Constants.BLOG_ENTRY_KEY, blogEntry);
			return new CommentConfirmationView();
		} else {
			setAttribute(Constants.BLOG_ENTRY_KEY, blogEntry);

			// is "remember me" set?
			Cookie rememberMe = CookieUtils.getCookie(request.getCookies(), "rememberMe");
			if (rememberMe != null) {
				setAttribute("rememberMe", "true");
			}

			ContentDecoratorContext decoratorContext = new ContentDecoratorContext();
			decoratorContext.setView(ContentDecoratorContext.DETAIL_VIEW);
			decoratorContext.setMedia(ContentDecoratorContext.HTML_PAGE);
			Comment comment = createBlankComment(blog, blogEntry, request);
			Comment decoratedComment = (Comment) comment.clone();
			blog.getContentDecoratorChain().decorate(decoratorContext, decoratedComment);
			setAttribute("decoratedComment", decoratedComment);
			setAttribute("undecoratedComment", comment);

			// are we replying to an existing comment?
			if (parentCommentId != null && parentCommentId.length() > 0) {
				Comment parentComment = blogEntry.getComment(Long.parseLong(parentCommentId));
				blog.getContentDecoratorChain().decorate(decoratorContext, parentComment);
				setAttribute("parentComment", parentComment);
			}

			return new CommentFormView();
		}
	}

	/**
	 * Adds a comment to an existing blog entry.
	 */
	@POST
	@Path("/comments")
	public View addComment(Comment comment) throws BlogServiceException {
		if (comment == null) { // comment is disabled?
			return new CommentConfirmationView();
		}
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		String rememberMe = request.getParameter("rememberMe");
		String submitType = request.getParameter("submit");

		ValidationContext context = validateComment(comment);

		// are we previewing or adding the comment?
		String previewButton = I18n.getMessage(blog, "comment.previewButton");

		ContentDecoratorContext decoratorContext = new ContentDecoratorContext();
		decoratorContext.setView(ContentDecoratorContext.DETAIL_VIEW);
		decoratorContext.setMedia(ContentDecoratorContext.HTML_PAGE);

		Comment decoratedComment = (Comment) comment.clone();
		blog.getContentDecoratorChain().decorate(decoratorContext, decoratedComment);
		setAttribute("decoratedComment", decoratedComment);
		setAttribute("undecoratedComment", comment);
		setAttribute("rememberMe", rememberMe);
		setAttribute(Constants.BLOG_ENTRY_KEY, comment.getBlogEntry());
		setAttribute(Constants.COMMENT_KEY, comment);
		request.getSession().setAttribute("rememberMe", request.getParameter("rememberMe"));

		if (submitType == null || submitType.equalsIgnoreCase(previewButton) || context.hasErrors()) {
			return new CommentFormView();
		} else {
			CommentConfirmationStrategy strategy = blog.getCommentConfirmationStrategy();

			Comment clonedComment = (Comment) comment.clone();
			request.getSession().setAttribute(Constants.COMMENT_KEY, comment);

			if (strategy.confirmationRequired(clonedComment, request)) {
				strategy.setupConfirmation(request);
				return new ConfirmCommentView();
			} else {
				saveComment(request, response, comment.getBlogEntry(), comment);
				request.getSession().removeAttribute(Constants.COMMENT_KEY);
				return new CommentConfirmationView();
			}
		}
	}

	private void saveComment(HttpServletRequest request, HttpServletResponse response, BlogEntry blogEntry,
			Comment comment) throws BlogServiceException {
		Blog blog = blogEntry.getBlog();
		blogEntry.addComment(comment);

		BlogService service = new BlogService();
		service.putBlogEntry(blogEntry);

		// remember me functionality
		String rememberMe = (String) request.getSession().getAttribute("rememberMe");
		if (rememberMe != null && rememberMe.equals("true")) {
			CookieUtils.addCookie(response, "rememberMe", "true", CookieUtils.ONE_MONTH);
			CookieUtils.addCookie(response, "rememberMe.author", encode(comment.getAuthor(), blog.getCharacterEncoding()),
					CookieUtils.ONE_MONTH);
			CookieUtils.addCookie(response, "rememberMe.email", encode(comment.getEmail(), blog.getCharacterEncoding()),
					CookieUtils.ONE_MONTH);
			CookieUtils.addCookie(response, "rememberMe.website", encode(comment.getWebsite(), blog.getCharacterEncoding()),
					CookieUtils.ONE_MONTH);
		} else {
			CookieUtils.removeCookie(response, "rememberMe");
			CookieUtils.removeCookie(response, "rememberMe.author");
			CookieUtils.removeCookie(response, "rememberMe.email");
			CookieUtils.removeCookie(response, "rememberMe.website");
		}
	}

	private String encode(String s, String characterEncoding) {
		if (s == null) {
			return "";
		} else {
			try {
				return URLEncoder.encode(s, characterEncoding);
			} catch (UnsupportedEncodingException e) {
				LOG.error("Exception encountered", e);
				return "";
			}
		}
	}

	private ValidationContext validateComment(Comment comment) {
		ValidationContext context = new ValidationContext();
		try {
			MailUtils.validate(comment.getEmail(), context);
		} catch (NoClassDefFoundError e) {
			// most likely: JavaMail is not in classpath
			// ignore, when we can not send email we must not validate address
			// this might lead to problems when mail is activated later without this
			// address being validated... Discussion started on mailing list, Oct-25
			// 2008
		}
		setAttribute("validationContext", context);
		return context;
	}

	/**
	 * Confirms a comment.
	 */
	@POST
	@Path("/comments/confirm")
	public View confirmComment() throws BlogServiceException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		BlogEntry blogEntry = null;
		Comment comment = null;

		comment = (Comment) request.getSession().getAttribute(Constants.COMMENT_KEY);
		String entry = comment.getBlogEntry().getId();

		BlogService service = new BlogService();
		blogEntry = service.getBlogEntry(blog, entry);
		if (blogEntry == null) {
			// just send back a 404 - this is probably somebody looking for a way
			// to send comment spam ;-)
			return new NotFoundView();
		} else if (!blogEntry.isCommentsEnabled()) { return new CommentConfirmationView(); }

		ContentDecoratorContext decoratorContext = new ContentDecoratorContext();
		decoratorContext.setView(ContentDecoratorContext.DETAIL_VIEW);
		decoratorContext.setMedia(ContentDecoratorContext.HTML_PAGE);

		Comment decoratedComment = (Comment) comment.clone();
		blog.getContentDecoratorChain().decorate(decoratorContext, decoratedComment);
		setAttribute("decoratedComment", decoratedComment);
		setAttribute("undecoratedComment", comment);
		setAttribute(Constants.BLOG_ENTRY_KEY, blogEntry);
		setAttribute(Constants.COMMENT_KEY, comment);

		CommentConfirmationStrategy strategy = blog.getCommentConfirmationStrategy();

		/* Comment clonedComment = (Comment) */comment.clone();

		if (strategy.isConfirmed(request)) {
			saveComment(request, response, blogEntry, comment);
			request.getSession().removeAttribute(Constants.COMMENT_KEY);
			return new CommentConfirmationView();
		} else {
			// try again!
			strategy.setupConfirmation(request);
			return new ConfirmCommentView();
		}
	}

	/** the number of responses to show per page */
	static final int PAGE_SIZE = 20;

	/**
	 * Allows the user to view all recently added responses.
	 */
	@GET
	@Path("/responses")
	public View responses(@QueryParam("page") @DefaultValue("1") int page,//
			@QueryParam("type") @DefaultValue("approved") String type) {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);

		final Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);

		List<String> responses = null;
		if ("pending".equalsIgnoreCase(type)) {
			responses = new ArrayList<String>(blog.getPendingResponses());
		} else if ("rejected".equalsIgnoreCase(type)) {
			responses = new ArrayList<String>(blog.getRejectedResponses());
		} else {
			responses = new ArrayList<String>(blog.getApprovedResponses());
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		Pageable pageable = new Pageable(responses) {
			@Override
			public List getListForPage() {
				List<Response> responses = new ArrayList<Response>();
				BlogService service = new BlogService();
				Iterator<String> it = super.getListForPage().iterator();
				while (it.hasNext()) {
					try {
						responses.add(service.getResponse(blog, it.next()));
					} catch (BlogServiceException e) {
						// do nothing - some responses just won't get shown,
						// but a message will be sent to the blog
					}
				}
				return responses;
			}
		};
		pageable.setPageSize(PAGE_SIZE);
		pageable.setPage(page);
		setAttribute("pageable", pageable);
		setAttribute("page", page);
		setAttribute("type", type);

		return new ResponsesView();
	}

	/**
	 * Finds a particular static page by its name, ready to be displayed.
	 */
	@GET
	@Path("/pages/{name}")
	public View pages(@PathParam("name") String name) throws StaticPageServiceException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);

		StaticPage staticPage = new StaticPageService().getStaticPageByName(blog, name);
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

	/**
	 * Allows the user to view the static pages associated with the current blog.
	 */
	@GET
	@Path("/pages")
	public View allPages() throws StaticPageServiceException {
		checkUserInRoles(Constants.BLOG_ADMIN_ROLE, Constants.BLOG_OWNER_ROLE, Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		StaticPageService service = new StaticPageService();
		setAttribute("staticPages", service.getStaticPages(blog));
		return new StaticPagesView();
	}

	/**
	 * Allows the user to manage (edit and remove) a static page.
	 */
	@POST
	@Path("/pages")
	public View managePage(StaticPage page, //
			@FormParam("submit") String submit, //
			@FormParam("confirm") String confirm) throws StaticPageServiceException {
		if ("Edit".equalsIgnoreCase(submit)) {//
			return editPage(page);
		} else if ("Remove".equalsIgnoreCase(submit) && "true".equals(confirm)) {//
			return removePage(page);
		} else if ("Unlock".equalsIgnoreCase(submit) && "true".equals(confirm)) {//
			return unlockPage(page);
		} else if ("Preview".equalsIgnoreCase(submit)) {//
			return previewPage(page);
		} else if ("Cancel".equalsIgnoreCase(submit)) {//
			return cancelPage(page);
		}
		// save static page
		// checkUserInRoles(Constants.BLOG_OWNER_ROLE);
		// return new RedirectView(page.getLocalPermalink());
		return savePage(page);
	}

	private View savePage(StaticPage staticPage) {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		StaticPageService service = new StaticPageService();
		populateStaticPage(staticPage, request);
		setAttribute(Constants.STATIC_PAGE_KEY, staticPage);

		ValidationContext validationContext = new ValidationContext();
		staticPage.validate(validationContext);

		if (validationContext.hasErrors()) {
			setAttribute("validationContext", validationContext);
			return new StaticPageFormView();
		} else {
			try {
				service.putStaticPage(staticPage);
				staticPage.getBlog().info(
						"Static page <a href=\"" + staticPage.getLocalPermalink() + "\">" + staticPage.getTitle() + "</a> saved.");
				service.unlock(staticPage);
				return new RedirectView(staticPage.getLocalPermalink());
			} catch (StaticPageServiceException e) {
				LOG.error(e.getMessage(), e);
				return new StaticPageFormView();
			}
		}
	}

	private View cancelPage(StaticPage staticPage) {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		StaticPageService service = new StaticPageService();
		service.unlock(staticPage);

		if (staticPage.isPersistent()) {
			return new RedirectView(staticPage.getLocalPermalink());
		} else {
			return new RedirectView(staticPage.getBlog().getUrl() + "p/pages");
		}
	}

	private View previewPage(StaticPage staticPage) {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		// we don't want to actually edit the original whilst previewing
		staticPage = (StaticPage) staticPage.clone();
		populateStaticPage(staticPage, request);

		ValidationContext validationContext = new ValidationContext();
		staticPage.validate(validationContext);
		setAttribute("validationContext", validationContext);
		setAttribute(Constants.STATIC_PAGE_KEY, staticPage);

		return new StaticPageFormView();
	}

	private void populateStaticPage(StaticPage staticPage, HttpServletRequest request) {
		String title = request.getParameter("title");
		String subtitle = request.getParameter("subtitle");
		String body = StringUtils.filterNewlines(request.getParameter("body"));
		String tags = request.getParameter("tags");
		String originalPermalink = request.getParameter("originalPermalink");
		String name = request.getParameter("name");
		String author = SecurityUtils.getUsername();
		String template = request.getParameter("template");

		staticPage.setTitle(title);
		staticPage.setSubtitle(subtitle);
		staticPage.setBody(body);
		staticPage.setTags(tags);
		staticPage.setAuthor(author);
		staticPage.setOriginalPermalink(originalPermalink);
		staticPage.setName(name);
		staticPage.setTemplate(template);
	}

	private View unlockPage(StaticPage staticPage) {
		checkUserInRoles(Constants.BLOG_ADMIN_ROLE, Constants.BLOG_OWNER_ROLE, Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		StaticPageService service = new StaticPageService();
		service.unlock(staticPage);
		blog.info("Static page <a href=\"" + staticPage.getLocalPermalink() + "\">" + staticPage.getTitle()
				+ "</a> unlocked.");
		return new RedirectView(blog.getUrl() + "p/pages");
	}

	private View removePage(StaticPage staticPage) throws StaticPageServiceException {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		StaticPageService service = new StaticPageService();
		if (service.lock(staticPage, SecurityUtils.getUsername())) {
			service.removeStaticPage(staticPage);
			blog.info("Static page \"" + staticPage.getTitle() + "\" removed.");
			service.unlock(staticPage);
		} else {
			setAttribute(Constants.STATIC_PAGE_KEY, staticPage);
			return new StaticPageLockedView();
		}

		return new RedirectView(blog.getUrl() + "p/pages");
	}

	private View editPage(StaticPage page) throws StaticPageServiceException {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		return editPage(page.getId());
	}

	/**
	 * Removes one or more existing static pages.
	 */
	@POST
	@Path("/pages/remove")
	public View removePages(@FormParam("page") String[] ids) {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		StaticPageService service = new StaticPageService();

		for (String id : ids) {
			try {
				removePage(service.getStaticPageById(blog, id));
			} catch (StaticPageServiceException e) {
				throw new WebApplicationException(e);
			}
		}

		return new RedirectView(blog.getUrl() + "p/pages");
	}

	/**
	 * Adds a new static page. This is called to create a blank static page to populate a HTML form containing the
	 * contents of that page.
	 */
	@GET
	@Path("/pages/add")
	public View addPage(@QueryParam("name") String name) {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);

		StaticPage staticPage = new StaticPage(blog);
		staticPage.setName(name);
		staticPage.setTitle("Title");
		staticPage.setBody("<p>\n\n</p>");
		staticPage.setAuthor(SecurityUtils.getUsername());

		setAttribute(Constants.STATIC_PAGE_KEY, staticPage);

		return new StaticPageFormView();
	}

	/**
	 * Edits an existing static page. This is called to populate a HTML form containing the contents of the static page.
	 */
	@GET
	@Path("/pages/edit/{name}")
	public View editPage(@PathParam("name") String name) throws StaticPageServiceException {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		StaticPageService service = new StaticPageService();
		StaticPage staticPage = service.getStaticPageById(blog, name);

		if (staticPage == null) {
			return new NotFoundView();
		} else {
			setAttribute(Constants.STATIC_PAGE_KEY, staticPage);
			if (service.lock(staticPage, SecurityUtils.getUsername())) {
				return new StaticPageFormView();
			} else {
				return new StaticPageLockedView();
			}
		}
	}

	/**
	 * Unlocks a static page.
	 */
	@GET
	@Path("/pages/unlock/{name}")
	public View unlockPage(@PathParam("name") String id) {
		checkUserInRoles(Constants.BLOG_ADMIN_ROLE, Constants.BLOG_OWNER_ROLE, Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);

		if (id != null) {
			StaticPageService service = new StaticPageService();
			try {
				StaticPage staticPage = service.getStaticPageById(blog, id);
				if (staticPage != null) {
					service.unlock(staticPage);
					blog.info("Static page <a href=\"" + staticPage.getLocalPermalink() + "\">" + staticPage.getTitle()
							+ "</a> unlocked.");
				}
			} catch (StaticPageServiceException e) {
				blog.warn(e.getClass().getName() + " Error while unlocking static page - "
						+ StringUtils.transformHTML(e.getMessage()));
				LOG.warn("Error while unlocking static page", e);
			}
		}

		return new RedirectView(blog.getUrl() + "p/pages");
	}

	/**
	 * Gets the RSS for a blog.
	 */
	@GET
	@Path("/feeds")
	public View feeds(@QueryParam("flavor") String flavor) {
		AbstractBlog blog = (AbstractBlog) request.getAttribute(Constants.BLOG_KEY);

		if (lastModifiedService.checkAndProcessLastModified(request, response, blog.getLastModified(), null)) { return new NotModifiedView(); }

		List<BlogEntry> blogEntries;
		String s = request.getParameter("includeAggregatedContent");
		boolean includeAggregatedContent = (s == null || s.equalsIgnoreCase("true"));

		if (blog instanceof Blog) {
			Tag tag = getTag((Blog) blog, request);
			Category category = getCategory((Blog) blog, request);
			String author = getAuthor(request);

			if (tag != null) {
				blogEntries = ((Blog) blog).getRecentPublishedBlogEntries(tag);
				setAttribute("tag", tag);
			} else if (category != null) {
				blogEntries = ((Blog) blog).getRecentPublishedBlogEntries(category);
				setAttribute("category", category);
			} else if (author != null) {
				blogEntries = ((Blog) blog).getRecentPublishedBlogEntries(author);
				setAttribute("author", author);
			} else {
				blogEntries = ((Blog) blog).getRecentPublishedBlogEntries();
			}
		} else {
			blogEntries = blog.getRecentBlogEntries();
		}

		List<BlogEntry> blogEntriesForFeed = new ArrayList<BlogEntry>();
		for (BlogEntry entry : blogEntries) {
			if (includeAggregatedContent || !entry.isAggregated()) {
				blogEntriesForFeed.add(entry);
			}
		}

		Collections.sort(blogEntriesForFeed, new BlogEntryComparator());

		setAttribute(Constants.BLOG_ENTRIES, blogEntriesForFeed);

		// set the locale of this feed request to be English
		javax.servlet.jsp.jstl.core.Config.set(request, javax.servlet.jsp.jstl.core.Config.FMT_LOCALE, Locale.ENGLISH);

		if (flavor != null && flavor.equalsIgnoreCase("atom")) {
			return new FeedView(AbstractRomeFeedView.FeedType.ATOM);
		} else if (flavor != null && flavor.equalsIgnoreCase("rdf")) {
			return new RdfView();
		} else {
			return new FeedView(AbstractRomeFeedView.FeedType.RSS);
		}
	}

	/**
	 * Helper method to find a named tag from a request parameter.
	 * 
	 * @param blog
	 *          the blog for which the feed is for
	 * @param request
	 *          the HTTP request containing the tag parameter
	 * @return a Tag instance, or null if the tag isn't specified or can't be
	 *         found
	 */
	private Tag getTag(Blog blog, HttpServletRequest request) {
		String tag = request.getParameter("tag");
		if (tag != null) {
			return new Tag(tag, blog);
		} else {
			return null;
		}
	}

	/**
	 * Helper method to find a named category from a request parameter.
	 * 
	 * @param blog
	 *          the blog for which the feed is for
	 * @param request
	 *          the HTTP request containing the category parameter
	 * @return a Category instance, or null if the category isn't specified or
	 *         can't be found
	 */
	private Category getCategory(Blog blog, HttpServletRequest request) {
		String categoryId = request.getParameter("category");
		if (categoryId != null) {
			return blog.getCategory(categoryId);
		} else {
			return null;
		}
	}

	/**
	 * Helper method to find a named author from a request parameter.
	 * 
	 * @param request
	 *          the HTTP request containing the tag parameter
	 * @return a String username, or null if the author isn't specified
	 */
	private String getAuthor(HttpServletRequest request) {
		return request.getParameter("author");
	}

	/**
	 * Displays a list of all users.
	 */
	@GET
	@Path("/users")
	public View users() throws SecurityRealmException {
		checkUserInRoles(Constants.BLOG_ADMIN_ROLE);
		Collection<PebbleUserDetails> users = PebbleContext.getInstance().getConfiguration().getSecurityRealm().getUsers();
		setAttribute("users", users);
		return new UsersView();
	}

	/**
	 * Displays information about a single user.
	 */
	@GET
	@Path("/users/{name}")
	public View user(@PathParam("name") String username) throws SecurityRealmException {
		checkUserInRoles(Constants.BLOG_ADMIN_ROLE);
		setAttribute("user", PebbleContext.getInstance().getConfiguration().getSecurityRealm().getUser(username));
		return new UserView();
	}

	/**
	 * Allows the user to see all feeds for the current blog, and subscribe via e-mail updates.
	 */
	@GET
	@Path("/subscribe")
	public View subscribe() {
		return new SubscribeView();
	}

	@POST
	@Path("/api/subscribe")
	public View subscribed(@FormParam("email") String email) {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		email = StringUtils.filterHTML(email);
		if (email != null && email.length() > 0) {
			blog.getEmailSubscriptionList().addEmailAddress(email);
			blog.info(email + " has subscribed to this blog.");

			setAttribute("email", email);
			return new SubscribedView();
		}
		return subscribe();
	}

	/**
	 * Performs a search on the current blog.
	 */
	@GET
	@Path("/search")
	public View searchView(@QueryParam("query") String q,//
			@QueryParam("title") String title, //
			@QueryParam("body") String body, //
			@QueryParam("category") String[] cateogries, //
			@QueryParam("author") String author, //
			@QueryParam("tags") String tags) throws SearchException, UnsupportedEncodingException {
		StringBuilder query = new StringBuilder();
		if (q != null) query.append(q.trim());
		addTerm(query, "title", title);
		addTerm(query, "body", body);
		addTerms(query, "category", cateogries);
		addTerm(query, "author", author);

		if (tags != null) {
			String s[] = tags.split(",");
			for (int i = 0; i < s.length; i++) {
				s[i] = Tag.encode(s[i].trim());
			}
			addTerms(query, "tag", s);
		}

		return query.length() == 0 ? new AdvancedSearchView() : search(query.toString());
	}

	private void addTerm(StringBuilder query, String key, String value) {
		if (value != null && value.trim().length() > 0) {
			if (query.length() > 0) {
				query.append(" AND ");
			}
			query.append(key + ":" + value.trim());
		}
	}

	private void addTerms(StringBuilder query, String key, String terms[]) {
		if (terms != null) {
			for (int i = 0; i < terms.length; i++) {
				addTerm(query, key, terms[i]);
			}
		}
	}

	public View search(String query) throws SearchException {
		return search(query, 1, null);
	}

	/**
	 * Performs a search on the current blog.
	 */
	@POST
	@Path("/search/do")
	public View search(@FormParam("query") String query, //
			@FormParam("page") @DefaultValue("1") int page, //
			@FormParam("sort") String sort) throws SearchException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		SearchResults results = blog.getSearchIndex().search(query);

		if (results.getNumberOfHits() == 1) {
			// if there is only one hit, redirect the user to it without the
			// search results page
			SearchHit hit = results.getHits().get(0);
			return new RedirectView(hit.getPermalink());
		} else {
			// show all results on the search results page
			if ("date".equalsIgnoreCase(sort)) {
				results.sortByDateDescending();
			} else {
				results.sortByScoreDescending();
			}

			Pageable<SearchHit> pageable = new Pageable<SearchHit>(results.getHits());
			pageable.setPageSize(PAGE_SIZE);
			pageable.setPage(page);

			try {
				setAttribute("searchResults", results);
				setAttribute("pageable", pageable);
				setAttribute("query", java.net.URLEncoder.encode(query, blog.getCharacterEncoding()));
			} catch (UnsupportedEncodingException uee) {
				LOG.error("Encoding error", uee);
			}

			return new SearchResultsView();
		}
	}

	/**
	 * Allows the user to view/edit the categories associated with the current blog.
	 */
	@GET
	@Path("/categories")
	public View allCategories() {
		if (isSecured && !User.isAuthenticated()) return new CategoriesView(false);
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		setAttribute(Constants.CATEGORY_KEY, new Category());
		setAttribute(Constants.CATEGORIES, blog.getCategories());
		return new CategoriesView(true);
	}

	/**
	 * Removes a given category from the associated with the current blog.
	 */
	@POST
	@Path("/categories/remove")
	public View removeCategories(@FormParam("category") String[] ids) {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		for (String id : ids) {
			if (!id.equals("/")) {
				Category category = blog.getCategory(id);
				if (category != null) {
					blog.removeCategory(category);
					try {
						// remove it from the persistent store
						DAOFactory factory = DAOFactory.getConfiguredFactory();
						CategoryDAO dao = factory.getCategoryDAO();
						dao.deleteCategory(category, blog);
					} catch (PersistenceException pe) {
						pe.printStackTrace();
					}
				}
			}
		}
		return allCategories();
	}

	/**
	 * Allows the user to edit a specific category.
	 */
	@GET
	@Path("/categories/of/{id}")
	public View editCategory(@PathParam("id") String id) throws SearchException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		if (!User.isAuthenticated()) { //
			return search("category:" + id, 1, "date");
		}
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		if (id != null && id.length() > 0) {
			Category category = blog.getCategory(id);
			setAttribute(Constants.CATEGORY_KEY, category);
		}
		setAttribute(Constants.CATEGORIES, blog.getCategories());
		return new CategoriesView(true);
	}

	/**
	 * Save a given category from the associated with the current blog.
	 */
	@POST
	@Path("/categories/save")
	public View saveCategory(Category category, @FormParam("name") String name, @FormParam("tags") String tags) {
		checkUserInRoles(Constants.BLOG_CONTRIBUTOR_ROLE);
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		category.setName(name);
		category.setTags(tags);
		try {
			// add it to the persistent store
			DAOFactory factory = DAOFactory.getConfiguredFactory();
			CategoryDAO dao = factory.getCategoryDAO();
			dao.addCategory(category, blog);
		} catch (PersistenceException pe) {
			pe.printStackTrace();
		}
		return allCategories();
	}

	/**
	 * An action to initiate a login.
	 */
	@GET
	@Path("/login")
	public View login(@QueryParam("redirectUrl") String redirectUrl) {
		if (isSecured && !User.isAuthenticated()) {
			setAttribute("error", request.getParameter("error"));
			return new LoginPageView();
		}
		AbstractBlog blog = (AbstractBlog) request.getAttribute(Constants.BLOG_KEY);
		if (redirectUrl == null || redirectUrl.trim().length() == 0) {
			redirectUrl = blog.getUrl();
		}
		return new RedirectView(redirectUrl);
	}

	/**
	 * Logs out the current user.
	 */
	@GET
	@Path("/logout")
	public View logout(@QueryParam("redirectUrl") String redirectUrl) {
		request.getSession().invalidate();

		// Cookie terminate = new
		// Cookie(TokenBasedRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY,
		// null);
		// terminate.setMaxAge(-1);
		// response.addCookie(terminate);

		return new RedirectView(redirectUrl);
	}
}
