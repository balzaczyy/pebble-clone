package cn.zhouyiyan.pebble;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.domain.BlogEntry;
import net.sourceforge.pebble.domain.BlogService;
import net.sourceforge.pebble.domain.BlogServiceException;
import net.sourceforge.pebble.domain.Comment;
import net.sourceforge.pebble.security.PebbleUserDetails;
import net.sourceforge.pebble.util.SecurityUtils;
import net.sourceforge.pebble.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class CommentReader implements MessageBodyReader<Comment> {
	private static final Logger LOG = LoggerFactory.getLogger(CommentReader.class);
	private final HttpServletRequest request;

	public CommentReader(@Context HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return Comment.class.equals(type);
	}

	@Override
	public Comment readFrom(Class<Comment> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		BlogEntry blogEntry;

		String entry = request.getParameter("entry");

		BlogService service = new BlogService();
		try {
			blogEntry = service.getBlogEntry(blog, entry);
		} catch (BlogServiceException e) {
			throw new WebApplicationException(e);
		}
		if (blogEntry == null) {
			// just send back a 404 - this is probably somebody looking for a way
			// to send comment spam ;-)
			LOG.info("ignoring saveComment with no related blog entry (spam) from " + request.getRemoteAddr());
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		return blogEntry.isCommentsEnabled() ? createComment(request, blogEntry) : null;
	}

	private Comment createComment(HttpServletRequest request, BlogEntry blogEntry) {
		String author = StringUtils.transformHTML(request.getParameter("author"));
		String email = request.getParameter("email");
		String website = request.getParameter("website");
		String avatar = request.getParameter("avatar");
		String ipAddress = request.getRemoteAddr();
		String title = StringUtils.transformHTML(request.getParameter("title"));
		String body = request.getParameter("commentBody");

		Comment comment = blogEntry.createComment(title, body, author, email, website, avatar, ipAddress);

		// if the user is authenticated, overwrite the author information
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
		}

		// are we replying to an existing comment?
		String parentCommentId = request.getParameter("comment");
		if (parentCommentId != null && parentCommentId.length() > 0) {
			long parent = Long.parseLong(parentCommentId);
			Comment parentComment = blogEntry.getComment(parent);
			if (parentComment != null) {
				comment.setParent(parentComment);
			}
		}

		return comment;
	}
}
