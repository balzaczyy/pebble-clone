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
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.domain.Category;

@Provider
public class CategoryReader implements MessageBodyReader<Category> {
	private final HttpServletRequest request;

	public CategoryReader(@Context HttpServletRequest request) {
		this.request = request;
	}
	
	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return Category.class.equals(type);
	}

	@Override
	public Category readFrom(Class<Category> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
    String id = request.getParameter("id");
		Category category = null;
    if (id != null && id.trim().length() > 0) {
			Blog blog = (Blog) request.getAttribute(Constants.BLOG_KEY);
			category = blog.getCategory(id);
      if (category == null) {
        // this is a new category
        category = new Category();
        category.setId(id);
				blog.addCategory(category);
			}
		}
		return category;
	}

}
