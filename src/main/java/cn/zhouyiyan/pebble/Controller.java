package cn.zhouyiyan.pebble;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.domain.AbstractBlog;
import net.sourceforge.pebble.domain.Blog;
import net.sourceforge.pebble.domain.MultiBlog;
import net.sourceforge.pebble.util.SecurityUtils;
import net.sourceforge.pebble.web.action.Action;
import net.sourceforge.pebble.web.action.ActionFactory;
import net.sourceforge.pebble.web.action.ActionNotFoundException;
import net.sourceforge.pebble.web.action.DefaultActionFactory;
import net.sourceforge.pebble.web.action.SecureAction;
import net.sourceforge.pebble.web.model.Model;
import net.sourceforge.pebble.web.security.SecurityTokenValidator;
import net.sourceforge.pebble.web.security.SecurityTokenValidatorImpl;
import net.sourceforge.pebble.web.view.View;
import net.sourceforge.pebble.web.view.impl.MultiBlogNotSupportedView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class Controller {
	private static final Logger log = LoggerFactory.getLogger(Controller.class);
	/**
	 * a reference to the factory used to create Action instances
	 */
	private final ActionFactory actionFactory = new DefaultActionFactory("action.properties");
	private final ActionFactory secureActionFactory = new DefaultActionFactory("secure-action.properties");

	/**
	 * The security token validator
	 */
	private final SecurityTokenValidator securityTokenValidator = new SecurityTokenValidatorImpl();

	@Context
	HttpServletRequest request;

	@Context
	HttpServletResponse response;

	@Context
	ServletContext servletContext;

	// @GET
	// public void process() {
	// System.out.println(request.getPathInfo());
	// }

	@POST
	public void post() throws IOException, ServletException {
		process();
	}

	@SuppressWarnings("deprecation")
	@GET
	// @Path("{actionName}.action")
	public void process(/* @PathParam("actionName") String actionName */) throws IOException, ServletException {
		AbstractBlog blog = (AbstractBlog) request.getAttribute(Constants.BLOG_KEY);

		// find which action should be used
		String actionName = request.getRequestURI();
		if (actionName.indexOf("?") > -1) {
			// strip of the query string - some servers leave this on
			actionName = actionName.substring(0, actionName.indexOf("?"));
		}
		int index = actionName.lastIndexOf("/");
		boolean isSecure = actionName.endsWith(".secureaction");
		actionName = actionName.substring(index + 1, (actionName.length() - (isSecure ? ".secureaction".length()
				: ".action".length())));
		Action action;

		try {
			log.debug("Action is " + actionName);
			action = isSecure ? secureActionFactory.getAction(actionName) : actionFactory.getAction(actionName);
		} catch (ActionNotFoundException anfe) {
			log.warn(anfe.getMessage());
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		boolean authorised = isAuthorised(request, action);
		if (!authorised) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		} else {
			boolean validated = securityTokenValidator.validateSecurityToken(request, response, action);
			if (!validated) {
				// Forward to no security url
				request.getRequestDispatcher("/noSecurityToken.action").forward(request, response);
			} else {
				try {
					Model model = new Model();
					model.put(Constants.BLOG_KEY, blog);
					model.put(Constants.BLOG_URL, blog.getUrl());
					action.setModel(model);
					View view;
					try {
						view = action.process(request, response);
					} catch (ClassCastException cce) {
						// PEBBLE-45 Actions intended for single blog mode should fail nicely. This is a simple method that will
						// allow has to handle all actions with minimal effort
						if (cce.getMessage().contains(MultiBlog.class.getName()) && cce.getMessage().contains(Blog.class.getName())) {
							view = new MultiBlogNotSupportedView();
						} else {
							throw cce;
						}
					}
					if (view != null) {

						view.setModel(model);
						view.setServletContext(servletContext);

						view.prepare();

						for (Object key : model.keySet()) {
							request.setAttribute(key.toString(), model.get(key.toString()));
						}

						response.setContentType(view.getContentType());
						view.dispatch(request, response, servletContext);

					}
				} catch (Exception e) {
					request.setAttribute("exception", e);
					throw new ServletException(e);
				}
			}
		}
	}

	private boolean isAuthorised(HttpServletRequest request, Action action) {
		if (action instanceof SecureAction) {
			SecureAction secureAction = (SecureAction) action;
			return isUserInRole(request, secureAction);
		} else {
			return true;
		}
	}

	/**
	 * Determines whether the current user in one of the roles specified by the secure action.
	 * 
	 * @param request
	 *          the HttpServletRequest
	 * @param action
	 *          the SecureAction to check against
	 * @return true if the user is in one of the roles, false otherwise
	 */
	private boolean isUserInRole(HttpServletRequest request, SecureAction action) {
		AbstractBlog ab = (AbstractBlog) request.getAttribute(Constants.BLOG_KEY);
		String currentUser = SecurityUtils.getUsername();
		String roles[] = action.getRoles(request);
		for (String role : roles) {
			if (role.equals(Constants.ANY_ROLE)) {
				return true;
			} else if (SecurityUtils.isUserInRole(role)) {
				if (ab instanceof Blog) {
					Blog blog = (Blog) ab;
					if (blog.isUserInRole(role, currentUser)) { return true; }
				} else {
					return true;
				}
			}
		}
		return false;
	}
}
