/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.test.web.server.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;
import org.springframework.web.util.UrlPathHelper;

/**
 * A Filter that will conditionally invoke a delegate {@link Filter} depending if the request URL matches,
 * &lt;url-pattern&gt;s as defined by the <a
 * href="http://download.oracle.com/otndocs/jcp/servlet-3.0-fr-oth-JSpec/">Servlet Specification</a>, any of the
 * {@link #addUrlPattern(String)}s.
 *
 * @author Rob Winch
 */
final class PatternMappingFilterProxy implements Filter {

	private static final String EXTENSION_MAPPING_PATTERN = "*.";

	private static final String PATH_MAPPING_PATTERN = "/*";

	private static final UrlPathHelper urlPathHelper = new UrlPathHelper();

	private final Filter delegate;

	/**
	 * &lt;url-pattern&gt; that require an exact match of the request URL. For example, "/test".
	 */
	private final List<String> exactMatches = new ArrayList<String>();

	/**
	 * &lt;url-pattern&gt; that require the request URL to start with a specific value. For example, "/test/*".
	 */
	private final List<String> startsWithMatches = new ArrayList<String>();

	/**
	 * &lt;url-pattern&gt; that require the request URL to end with a specific value. For example, "*.html".
	 */
	private final List<String> endsWithMatches = new ArrayList<String>();

	/**
	 * Creates a new instance
	 * @param delegate the {@link Filter} to delegate to if the urlPattern matches. Cannot be null.
	 * @param urlPatterns the urlPatterns to add. Cannot be null.
	 */
	public PatternMappingFilterProxy(Filter delegate, String... urlPatterns) {
		Assert.notNull(delegate);
		this.delegate = delegate;
		for(String urlPattern : urlPatterns) {
		    addUrlPattern(urlPattern);
		}
	}

	/**
	 * Adds a new &lt;url-pattern&gt; that will invoke the delegate {@link Filter}.
	 * @param urlPattern
	 */
	private void addUrlPattern(String urlPattern) {
		Assert.notNull(urlPattern, "urlPattern cannot be null");
		if(urlPattern.startsWith(EXTENSION_MAPPING_PATTERN)) {
			endsWithMatches.add(urlPattern.substring(1, urlPattern.length()));
		} else if(urlPattern.equals(PATH_MAPPING_PATTERN)) {
			startsWithMatches.add("");
		}
		else if (urlPattern.endsWith(PATH_MAPPING_PATTERN)) {
			startsWithMatches.add(urlPattern.substring(0, urlPattern.length() - 1));
			exactMatches.add(urlPattern.substring(0, urlPattern.length() - 2));
		} else {
			if("".equals(urlPattern)) {
				urlPattern = "/";
			}
			exactMatches.add(urlPattern);
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
			ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String requestPath = urlPathHelper.getPathWithinApplication(httpRequest);

		if(matches(requestPath)) {
			delegate.doFilter(request, response, filterChain);
		} else {
			filterChain.doFilter(request, response);
		}
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		delegate.init(filterConfig);
	}

	public void destroy() {
		delegate.destroy();
	}

	/**
	 * Determines if the delegate {@link Filter} should be invoked.
	 * @param requestPath
	 * @return true if the delegate {@link Filter} should be invoked, false if should continue the {@link FilterChain}.
	 */
	private boolean matches(String requestPath) {
		for(String exactMatch : exactMatches) {
			if(exactMatch.equals(requestPath)) {
				return true;
			}
		}
		if(!requestPath.startsWith("/")) {
			return false;
		}
		for(String endsWithMatch : endsWithMatches) {
			if(requestPath.endsWith(endsWithMatch)) {
				return true;
			}
		}
		for(String startsWithMatch : startsWithMatches) {
			if(requestPath.startsWith(startsWithMatch)) {
				return true;
			}
		}
		return false;
	}

}