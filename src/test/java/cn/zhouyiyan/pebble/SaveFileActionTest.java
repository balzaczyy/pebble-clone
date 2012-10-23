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
package cn.zhouyiyan.pebble;

import javax.ws.rs.WebApplicationException;

import net.sourceforge.pebble.domain.FileManager;
import net.sourceforge.pebble.domain.FileMetaData;
import net.sourceforge.pebble.web.action.SecureActionTestCase;
import net.sourceforge.pebble.web.view.View;
import net.sourceforge.pebble.web.view.impl.FileFormView;

/**
 * Tests for the SaveFileAction class.
 *
 * @author    Simon Brown
 */
public class SaveFileActionTest extends SecureActionTestCase {
	private Blogs blogs;
  @Override
	protected void setUp() throws Exception {
    super.setUp();
		blogs = new Blogs();
		blogs.isSecured = false;
		blogs.request = request;
  }

  /**
   * Tests that a file can be saved.
   */
  public void testSaveFile() throws Exception {
		// request.setParameter("path", "/");
		// request.setParameter("name", "afile.txt");
		// request.setParameter("type", FileMetaData.BLOG_FILE);
		// request.setParameter("fileContent", "Some content.");

		View view = blogs.saveFile(FileMetaData.BLOG_FILE, "/", "afile.txt", "Some content.");

    // check file was saved and the right view is returned
    FileManager fileManager = new FileManager(blog, FileMetaData.BLOG_FILE);
    assertEquals("Some content.", fileManager.loadFile("/", "afile.txt"));
		assertTrue(view instanceof FileFormView);

    // and clean up
    fileManager.deleteFile("/", "afile.txt");
  }

  /**
   * Tests that a file can't be saved outside of the root.
   */
  public void testSaveFileReturnsForbiddenWheOutsideOfRoot() throws Exception {
		// request.setParameter("path", "/");
		// request.setParameter("name", "../afile.txt");
		// request.setParameter("fileContent", "some content");
		// request.setParameter("type", FileMetaData.BLOG_FILE);

		try {
			blogs.saveFile(FileMetaData.BLOG_FILE, "/", "../afile.txt", "some content");
			fail();
		} catch (WebApplicationException e) {
			assertEquals(403, e.getResponse().getStatus());
		}
  }

}
