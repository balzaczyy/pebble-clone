/*
 * Copyright (c) 2003-2011, Simon Brown
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *   - Neither the name of Pebble nor the names of its contributors may
 *     be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.sourceforge.pebble.web.action;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A factory class from which to look up and retrieve an instance
 * of an Action class to process a specific request.
 *
 * @author    Simon Brown
 */
public class DefaultActionFactory implements ActionFactory {
  /** the log used by this class */
  private static Log log = LogFactory.getLog(DefaultActionFactory.class);

  /** the collection of actions that we know about */
	private final Properties actions = new Properties();

  /**
	 * Initializes this component, reading in and creating the map of action names
	 * to action classes.
	 * 
	 * @throws IOException
	 *           If an error occurs looking up all the actions
	 */
	public DefaultActionFactory(String actionMappingFileName) {
    // Load all resources by the given name, allows for plugins to provide their own actions
		ClassLoader loader = getClass().getClassLoader();
		URL url = loader.getResource(actionMappingFileName);

      // load the properties file containing the name -> class name mapping
      InputStream in = null;
      try {
        in = url.openStream();
			actions.load(in);
      } catch (IOException ioe) {
        log.error("Error reading actions for class: " + url, ioe);
      } finally {
        IOUtils.closeQuietly(in);
      }
  }

  /**
   * Given the name/type of request, this method returns the Action
   * instance appropriate to process the request.
   *
   * @param name    the name (type) of the request
   * @return  an instance of Action (could be null if no mapping has been defined)
   */
  public Action getAction(String name) throws ActionNotFoundException {
    try {
      // instantiate the appropriate class to handle the request
      if (actions.containsKey(name)) {
				Class<?> c = getClass().getClassLoader().loadClass(actions.getProperty(name));
        Class<? extends Action> actionClass = c.asSubclass(Action.class);
				return actionClass.newInstance();
      } else {
        throw new ActionNotFoundException("An action called " + name + " could not be found");
      }
    } catch (ClassNotFoundException cnfe) {
      log.error(cnfe.getMessage(), cnfe);
      throw new ActionNotFoundException("An action called " + name + " could not be loaded", cnfe);
		} catch (InstantiationException be) {
      log.error(be.getMessage(), be);
      throw new ActionNotFoundException("An action called " + name + " could not be instantiated", be);
		} catch (IllegalAccessException e) {
			log.error(e.getMessage(), e);
			throw new ActionNotFoundException("An action called " + name + " could not be accessed", e);
		}
  }
}