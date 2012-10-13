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

import static org.mockito.Mockito.mock;

import java.util.Collection;

import net.sourceforge.pebble.Constants;
import net.sourceforge.pebble.domain.BlogEntry;
import net.sourceforge.pebble.domain.BlogService;
import net.sourceforge.pebble.domain.Category;
import net.sourceforge.pebble.domain.MultiBlogTestCase;
import net.sourceforge.pebble.mock.MockHttpServletRequest;
import net.sourceforge.pebble.mock.MockHttpServletResponse;
import net.sourceforge.pebble.service.LastModifiedService;

/**
 * Tests for the FeedAction class.
 *
 * @author    Simon Brown
 */
public class MultiBlogFeedActionTest extends MultiBlogTestCase {

	private Blogs blogs;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private LastModifiedService lastModifiedService;

  @Override
	protected void setUp() throws Exception {
    super.setUp();

    request = new MockHttpServletRequest();
		request.setAttribute(Constants.BLOG_KEY, blog1);
    response = new MockHttpServletResponse();
    lastModifiedService = mock(LastModifiedService.class);

		blogs = new Blogs();
		blogs.request = request;
		blogs.response = response;
		blogs.lastModifiedService = lastModifiedService;
  }

  public void testCategoriesExcludedFromFeedInMultiUserMode() throws Exception {
    Category cat1 = new Category("/cat1", "Category 1");
    Category cat2 = new Category("/cat2", "Category 2");
    blog1.addCategory(cat1);
    blog1.addCategory(cat2);

    BlogService service = new BlogService();

    BlogEntry entry1 = new BlogEntry(blog1);
    entry1.addCategory(cat1);
    entry1.setPublished(true);
    service.putBlogEntry(entry1);

    BlogEntry entry2 = new BlogEntry(blog1);
    entry2.addCategory(cat2);
    entry2.setPublished(true);
    service.putBlogEntry(entry2);

    request.setParameter("category", "/cat2");
		blogs.feeds(null);
		Category category = (Category) request.getAttribute("category");
		Collection<?> entries = (Collection<?>) request.getAttribute("blogEntries");

    assertEquals(cat2, category);
    assertFalse(entries.contains(entry1));
    assertTrue(entries.contains(entry2));
  }

}
