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
import net.sourceforge.pebble.domain.StaticPage;
import net.sourceforge.pebble.service.StaticPageService;
import net.sourceforge.pebble.service.StaticPageServiceException;

@Provider
public class StaticPageReader implements MessageBodyReader<StaticPage> {
	private final HttpServletRequest request;

	public StaticPageReader(@Context HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return StaticPage.class.equals(type);
	}

	@Override
	public StaticPage readFrom(Class<StaticPage> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
		Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
		String id = request.getParameter("page");
		String persistent = request.getParameter("persistent");

		if ("true".equalsIgnoreCase(persistent)) {
			StaticPageService service = new StaticPageService();
			StaticPage staticPage = null;
			try {
				staticPage = service.getStaticPageById(blog, id);
			} catch (StaticPageServiceException e) {
				throw new WebApplicationException(e);
			}
			if (staticPage == null) throw new WebApplicationException(Status.NOT_FOUND);
			return staticPage;
		}
		return new StaticPage(blog);
	}

}
