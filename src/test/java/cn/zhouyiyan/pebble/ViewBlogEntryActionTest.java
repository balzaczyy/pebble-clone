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

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.domain.BlogEntry;
import net.sourceforge.pebble.domain.BlogService;
import net.sourceforge.pebble.util.SecurityUtils;
import net.sourceforge.pebble.web.action.SingleBlogActionTestCase;
import net.sourceforge.pebble.web.view.NotFoundView;
import net.sourceforge.pebble.web.view.View;
import net.sourceforge.pebble.web.view.impl.BlogEntryView;

import org.junit.Ignore;

/**
 * Tests for the ViewBlogEntryAction class.
 *
 * @author    Simon Brown
 */
public class ViewBlogEntryActionTest extends SingleBlogActionTestCase {
	private Blogs blogs;

  @Override
	protected void setUp() throws Exception {
    super.setUp();
		blogs = new Blogs();
		blogs.request = request;
		// blogs.response = response;
  }

	@Ignore("impossible")
  public void testViewBlogEntryWithNullId() throws Exception {
		// View view = blogs.getEntry(0, 0, 0, null);
		// assertTrue(view instanceof NotFoundView);
  }

  public void testViewNonExistentBlogEntry() throws Exception {
		// request.setParameter("entry", "1234567890123");
		View view = blogs.getEntry(0, 0, 0, "1234567890123.html");

    assertTrue(view instanceof NotFoundView);
  }

  public void testPublishedViewBlogEntry() throws Exception {
    BlogEntry blogEntry1 = new BlogEntry(blog);
    blogEntry1.setPublished(true);
    BlogService service = new BlogService();
    service.putBlogEntry(blogEntry1);

    SecurityUtils.runAsUnauthenticated();
		View view = blogs.getEntry(0, 0, 0, blogEntry1.getId() + ".html");

		BlogEntry blogEntry2 = (BlogEntry) request.getAttribute(Constants.BLOG_ENTRY_KEY);
    assertEquals(blogEntry1.getId(), blogEntry2.getId());
    assertTrue(view instanceof BlogEntryView);
  }

  public void testUnpublishedViewBlogEntryAsAnonymousUser() throws Exception {
    BlogEntry blogEntry1 = new BlogEntry(blog);
    blogEntry1.setPublished(false);
    BlogService service = new BlogService();
    service.putBlogEntry(blogEntry1);

    SecurityUtils.runAsAnonymous();
		View view = blogs.getEntry(0, 0, 0, blogEntry1.getId() + ".html");

		BlogEntry blogEntry2 = (BlogEntry) request.getAttribute(Constants.BLOG_ENTRY_KEY);
    assertNull(blogEntry2);
    assertTrue(view instanceof NotFoundView);
  }

  public void testUnpublishedViewBlogEntryAsUserThatIsAuthorisedForBlog() throws Exception {
    BlogEntry blogEntry1 = new BlogEntry(blog);
    blogEntry1.setPublished(false);
    BlogService service = new BlogService();
    service.putBlogEntry(blogEntry1);

    SecurityUtils.runAsBlogContributor();
		View view = blogs.getEntry(0, 0, 0, blogEntry1.getId() + ".html");

		BlogEntry blogEntry2 = (BlogEntry) request.getAttribute(Constants.BLOG_ENTRY_KEY);
    assertEquals(blogEntry1.getId(), blogEntry2.getId());
    assertTrue(view instanceof BlogEntryView);
  }

}
