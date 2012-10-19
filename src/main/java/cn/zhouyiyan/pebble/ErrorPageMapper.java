package cn.zhouyiyan.pebble;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.web.view.impl.FourZeroFourView;
import net.sourceforge.pebble.web.view.impl.FourZeroOneView;
import net.sourceforge.pebble.web.view.impl.FourZeroThreeView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redirects the user to an error page.
 */
@Provider
public class ErrorPageMapper implements ExceptionMapper<WebApplicationException> {
	private static final Logger LOG = LoggerFactory.getLogger(ErrorPageMapper.class);
	@Context
	HttpServletRequest request;

	@Override
	public Response toResponse(WebApplicationException exception) {
		switch (exception.getResponse().getStatus()) {
		case 401:
			return Response.ok(new FourZeroOneView()).build();
		case 403:
			return Response.ok(new FourZeroThreeView()).build();
		case 404:
			LOG.warn("URL is {}", request.getAttribute(Constants.EXTERNAL_URI));
			return Response.ok(new FourZeroFourView()).build();
		case 500:
			// TODO is it possible?
			GeneralExceptionMapper gem = new GeneralExceptionMapper();
			gem.request = request;
			return gem.toResponse(exception);
		}
		return Response.fromResponse(exception.getResponse()).build();
	}
}
