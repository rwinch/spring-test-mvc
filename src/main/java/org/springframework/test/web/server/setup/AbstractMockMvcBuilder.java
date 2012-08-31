/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.server.setup;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.core.NestedRuntimeException;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.web.server.MockFilterChain;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.TestDispatcherServlet;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * An abstract class for building {@link MockMvc} instances.
 *
 * <p>
 * Provides basic support for using {@link Filter}s with &lt;url-pattern&lt; as defined by the <a
 * href="http://download.oracle.com/otndocs/jcp/servlet-3.0-fr-oth-JSpec/">Servlet Specification</a>. Since
 * {@link MockMvc} only processes requests, other dispatch types like forward, include, error are not supported.
 * Additionally, since only the Spring {@link DispatcherServlet} is supported, filtering over specific {@link Servlet}s
 * is not supported.
 * </p>
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 */
@SuppressWarnings("deprecation")
public abstract class AbstractMockMvcBuilder implements MockMvcBuilder {

	private List<Filter> filters = new ArrayList<Filter>();

	/**
	 * Build a {@link MockMvc} instance.
	 */
	public final MockMvc build() {

		ServletContext servletContext = initServletContext();
		WebApplicationContext wac = initWebApplicationContext(servletContext);

		ServletConfig config = new MockServletConfig(servletContext);
		TestDispatcherServlet dispatcherServlet = new TestDispatcherServlet(wac);
		try {
			dispatcherServlet.init(config);
		}
		catch (ServletException ex) {
			// should never happen..
			throw new MockMvcBuildException("Failed to init DispatcherServlet", ex);
		}

		MockFilterChain mockMvcFilterChain = new MockFilterChain(dispatcherServlet, filters.toArray(new Filter[filters.size()])) {};
		return new MockMvc(mockMvcFilterChain, dispatcherServlet.getServletContext()) {};
	}

	/**
	 * Add filter mappings to all requests for each of the {@link Filter} objects. This is the equivalent of specifying
	 * a &lt;url-pattern&gt; of "/*" for each {@link Filter}. For example:
	 *
	 * <pre class="code">
	 * mockMvcBuilder.addFilters(springSecurityFilterChain);
	 * </pre>
	 *
	 * is the equivalent of
	 *
	 * <pre class="code">
	 * &lt;filter-mapping&gt;
	 *     &lt;filter-name&gt;springSecurityFilterChain&lt;/filter-name&gt;
	 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
	 * &lt;/filter-mapping&gt;
	 * </pre>
	 *
	 * <p>
	 * Remember that just as it is when defining {@link Filter} mappings in a container, order matters.
	 * </p>
	 * @param filter
	 * @param filters
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T extends AbstractMockMvcBuilder> T addFilters(Filter... filters) {
		Assert.notNull(filters, "filters cannot be null");

		for(Filter f : filters) {
			Assert.notNull(f, "filters cannot contain null values");
			this.filters.add(f);
		}
		return (T) this;
	}

	/**
	 * Add a {@link Filter} mapping for a specific set of url patterns. For example:
	 *
	 * <pre class="code">
	 * mockMvcBuilder.addFilters(myResourceFilter, "/resources/*");
	 * </pre>
	 *
	 * is the equivalent of
	 *
	 * <pre class="code">
	 * &lt;filter-mapping&gt;
	 *     &lt;filter-name&gt;myResourceFilter&lt;/filter-name&gt;
	 *     &lt;url-pattern&gt;/resources/*&lt;/url-pattern&gt;
	 * &lt;/filter-mapping&gt;
	 * </pre>
	 *
	 * <p>
	 * Remember that just as it is when defining {@link Filter} mappings in a container, order matters.
	 * </p>
	 * @param filter The {@link Filter} to add.
	 * @param urlPatterns The url patterns to match on. If this is empty, it will default to matching every URL (i.e. "/*").
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T extends AbstractMockMvcBuilder> T addFilter(Filter filter, String... urlPatterns) {
		Assert.notNull(filter, "filter cannot be null");
		Assert.notNull(urlPatterns, "urlPatterns cannot be null");

		if(urlPatterns.length > 0) {
			filter = new PatternMappingFilterProxy(filter, urlPatterns);
		}

		this.filters.add(filter);
		return (T) this;
	}

	/**
	 * Return ServletContext to use, never {@code null}.
	 */
	protected abstract ServletContext initServletContext();

	/**
	 * Return the WebApplicationContext to use, possibly {@code null}.
	 * @param servletContext the ServletContext returned
	 * from {@link #initServletContext()}
	 */
	protected abstract WebApplicationContext initWebApplicationContext(ServletContext servletContext);


	@SuppressWarnings("serial")
	private static class MockMvcBuildException extends NestedRuntimeException {

		public MockMvcBuildException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}
}
