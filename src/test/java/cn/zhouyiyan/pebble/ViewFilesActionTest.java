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

import java.util.List;

import javax.ws.rs.WebApplicationException;

import net.sourceforge.pebble.domain.FileManager;
import net.sourceforge.pebble.domain.FileMetaData;
import net.sourceforge.pebble.domain.Theme;
import net.sourceforge.pebble.web.action.SecureActionTestCase;
import net.sourceforge.pebble.web.view.View;
import net.sourceforge.pebble.web.view.impl.FilesView;

/**
 * Tests for the ViewFilesAction class.
 *
 * @author    Simon Brown
 */
public class ViewFilesActionTest extends SecureActionTestCase {
	private Blogs blogs;
  @Override
	protected void setUp() throws Exception {
    super.setUp();
		blogs = new Blogs();
		blogs.request = request;
		blogs.isSecured = false;
  }

  /**
   * Tests that files can be accessed.
   */
  public void testViewFiles() throws Exception {
    // create some files and directories
    FileManager fileManager = new FileManager(blog, FileMetaData.BLOG_FILE);
    fileManager.createDirectory("/", "a");
    fileManager.createDirectory("/", "z");
    fileManager.saveFile("/", "y.txt", "Some content");
    fileManager.saveFile("/", "b.txt", "Some content");

		// request.setParameter("path", "/");
		// request.setParameter("type", FileMetaData.BLOG_FILE);

		View view = blogs.allFiles(FileMetaData.BLOG_FILE, "/");

    // check file information available and the right view is returned
		assertEquals(FileMetaData.BLOG_FILE, request.getAttribute("type"));
		FileMetaData fileMetaData = (FileMetaData) request.getAttribute("directory");
    assertEquals("/", fileMetaData.getAbsolutePath());
    assertTrue(fileMetaData.isDirectory());
    assertTrue(view instanceof FilesView);

		@SuppressWarnings("unchecked")
		List<FileMetaData> files = (List<FileMetaData>) request.getAttribute("files");
    assertEquals(4, files.size());

    // the files should be in this order
    // - a, z, b.txt, y.txt (directories followed by files, both alphabetically)
    FileMetaData file = files.get(0);
    assertEquals("a", file.getName());
    assertEquals("/", file.getPath());
    assertTrue(file.isDirectory());
    file = files.get(1);
    assertEquals("z", file.getName());
    assertEquals("/", file.getPath());
    assertTrue(file.isDirectory());
    file = files.get(2);
    assertEquals("b.txt", file.getName());
    assertEquals("/", file.getPath());
    assertFalse(file.isDirectory());
    file = files.get(3);
    assertEquals("y.txt", file.getName());
    assertEquals("/", file.getPath());
    assertFalse(file.isDirectory());

    // and clean up
    fileManager.deleteFile("/", "a");
    fileManager.deleteFile("/", "z");
    fileManager.deleteFile("/", "y.txt");
    fileManager.deleteFile("/", "b.txt");
  }

  /**
   * Tests that files can be accessed, even when the path isn't specified.
   */
  public void testViewFilesWhenPathNotSpecified() throws Exception {
    // create some files and directories
    FileManager fileManager = new FileManager(blog, FileMetaData.BLOG_FILE);
    fileManager.saveFile("/", "a.txt", "Some content");

		// request.setParameter("type", FileMetaData.BLOG_FILE);
		blogs.allFiles(FileMetaData.BLOG_FILE);
		@SuppressWarnings("unchecked")
		List<FileMetaData> files = (List<FileMetaData>) request.getAttribute("files");
    assertEquals(1, files.size());
    FileMetaData file = files.get(0);
    assertEquals("a.txt", file.getName());
    assertEquals("/", file.getPath());
    assertFalse(file.isDirectory());

    // and clean up
    fileManager.deleteFile("/", "a.txt");
  }

  /**
   * Tests that the upload action is set correctly for blog images.
   */
  public void testUploadActionForBlogImages() throws Exception {
		// request.setParameter("path", "/");
		// request.setParameter("type", FileMetaData.BLOG_IMAGE);
		blogs.allFiles(FileMetaData.BLOG_IMAGE, "/");
  }

  /**
   * Tests that the upload action is set correctly for theme files.
   */
  public void testUploadActionForThemeFiles() throws Exception {
		// request.setParameter("path", "/");
		// request.setParameter("type", FileMetaData.THEME_FILE);
    Theme theme = new Theme(blog, "custom", "/some/path");
    blog.setEditableTheme(theme);
		blogs.allFiles(FileMetaData.THEME_FILE, "/");
  }

  /**
   * Tests that files can't be accessed outside of the root.
   */
  public void testViewFilesReturnsForbiddenWhenOutsideOfRoot() throws Exception {
		// request.setParameter("path", "../");
		// request.setParameter("type", FileMetaData.BLOG_FILE);

		try {
			blogs.allFiles(FileMetaData.BLOG_FILE, "../");
			fail();
		} catch (WebApplicationException e) {
			assertEquals(403, e.getResponse().getStatus());
		}
  }

  /**
   * Tests that a specific file can be selected.
   */
  public void testSelectFile() throws Exception {
		// request.setParameter("path", "/");
		// request.setParameter("type", FileMetaData.BLOG_IMAGE);
		// request.setParameter("file", "afile.txt");
		blogs.allFiles(FileMetaData.BLOG_IMAGE, "/", "afile.txt");
		FileMetaData fileMetaData = (FileMetaData) request.getAttribute("file");
    assertEquals("afile.txt", fileMetaData.getName());
    assertFalse(fileMetaData.isDirectory());
  }

}
