package cn.zhouyiyan.pebble;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import net.sourceforge.pebble.web.model.Model;
import net.sourceforge.pebble.web.view.View;

@Provider
@Produces("text/html")
public class ViewWriter implements MessageBodyWriter<View> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return true;
	}

	@Override
	public long getSize(View t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Context
	ServletContext servletContext;

	@Context
	HttpServletResponse response;

	@Context
	HttpServletRequest request;

	@Override
	public void writeTo(View t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
			WebApplicationException {
		if (t != null) try {
			final HttpServletRequest thisRequest = request;
			t.setModel(new Model() {
				@Override
				public Object get(String name) {
					return thisRequest.getAttribute(name);
				}

				@Override
				public void put(String name, Object value) {
					// TODO merge with Blogs.setAttribute
					@SuppressWarnings("unchecked")
					Set<String> vmKeys = (Set<String>) request.getAttribute("vmkeys");
					if (vmKeys == null) {
						vmKeys = new HashSet<String>();
						request.setAttribute("vmkeys", vmKeys);
					}
					vmKeys.add(name);
					request.setAttribute(name, value);
				}
			});
			t.setServletContext(servletContext);
			t.prepare();
			response.setContentType(t.getContentType());
			t.dispatch(request, response, servletContext);
		} catch (ServletException e) {
			throw new WebApplicationException(e);
		}
	}

}
