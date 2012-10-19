package cn.zhouyiyan.pebble;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import net.sourceforge.pebble.util.ExceptionUtils;
import net.sourceforge.pebble.web.view.impl.ErrorView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {
	private static final Logger LOG = LoggerFactory.getLogger(GeneralExceptionMapper.class);
	@Context
	HttpServletRequest request;

	@Override
	public Response toResponse(Exception e) {
		if (e == null) {
			Object exceptionAttribute = request.getAttribute("javax.servlet.error.exception");
			if (exceptionAttribute instanceof Exception) {
				e = (Exception) exceptionAttribute;
			}
		}
		if (e != null) {
			LOG.error("Request URL = {}", request.getRequestURL());
			LOG.error("Request URI = {}", request.getRequestURI());
			LOG.error("Query string = {}", request.getQueryString());
			Enumeration<?> en = request.getHeaderNames();
			while (en.hasMoreElements()) {
				String headerName = (String) en.nextElement();
				LOG.error("{} = {}", headerName, request.getHeader("headerName"));
			}
			LOG.error("Parameters :");
			Enumeration<?> names = request.getParameterNames();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				LOG.error(" {} = {}", name, request.getParameter(name));
			}

			LOG.error("Exception encountered", e);

			Blogs.setAttribute(request, "stackTrace", ExceptionUtils.getStackTraceAsString(e));
		}

		return Response.ok(new ErrorView()).build();
	}

}
