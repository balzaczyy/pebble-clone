package cn.zhouyiyan.pebble;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import net.sourceforge.pebble.web.view.View;

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
			t.setServletContext(servletContext);
			t.prepare();
			response.setContentType(t.getContentType());
			t.dispatch(request, response, servletContext);
		} catch (ServletException e) {
			throw new WebApplicationException(e);
		}
	}

}
