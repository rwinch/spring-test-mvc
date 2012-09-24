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
package org.springframework.test.web.server.samples.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.web.server.request.RequestPostProcessor;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Example {@link RequestPostProcessor} implementations that allow the request to be
 * already authenticated to Spring Security. While these are just examples,
 * <a href="https://jira.springsource.org/browse/SEC-2015">official support</a> for
 * Spring Security is planned.
 *
 * @author Rob Winch
 */
final class SecurityRequestPostProcessors {

	/**
	 * Post processes the {@link MockHttpServletRequest} to authenticate as the user with
	 * the specified username. All details are declarative and do not require that the
	 * user actually exists. This means that the authorities or roles need to be
	 * specified too.
	 *
	 * @param username
	 * @return
	 */
	public static UserRequestPostProcessor user(String username) {
		return new UserRequestPostProcessor(username);
	}

	/**
	 * Post processes the {@link MockHttpServletRequest} to authenticate as the user with
	 * the specified username. The additional details are obtained from the
	 * {@link UserDetailsService} provided within the {@link WebApplicationContext}.
	 *
	 * @param username
	 * @return
	 */
	public static UserDetailsRequestPostProcessor userDeatilsService(String username) {
		return new UserDetailsRequestPostProcessor(username);
	}

	/**
	 * Post processes the {@link MockHttpServletRequest} to use the {@link SecurityContext} and thus be
	 * authenticated with {@link SecurityContext#getAuthentication()}.
	 *
	 * @param securityContext
	 * @return
	 */
	public SecurityContextRequestPostProcessor securityContext(SecurityContext securityContext) {
		return new SecurityContextRequestPostProcessor(securityContext);
	}

	public final static class SecurityContextRequestPostProcessor extends AbstractSecurityContextRequestPostProcessor {
		private final SecurityContext securityContext;

		private SecurityContextRequestPostProcessor(SecurityContext securityContext) {
			this.securityContext = securityContext;
		}

		public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
			save(securityContext,request);
			return request;
		}
	}

	public final static class UserRequestPostProcessor extends AbstractSecurityContextRequestPostProcessor {
		private final String username;
		private String rolePrefix = "ROLE_";
		private Object credentials;
		private List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

		private UserRequestPostProcessor(String username) {
			Assert.notNull(username, "username cannot be null");
			this.username = username;
		}

		public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, credentials, authorities);
			save(authentication,request);
			return request;
		}

		/**
		 * Sets the prefix to append to each role if the role does not already start with
		 * the prefix. If no prefix is desired, an empty String or null can be used.
		 * @param rolePrefix
		 * @return
		 */
		public UserRequestPostProcessor rolePrefix(String rolePrefix) {
			this.rolePrefix = rolePrefix;
			return this;
		}

		/**
		 * Specify the roles of the user to authenticate as. This method is similar to the
		 * {@link #authorities(GrantedAuthority...)}, but just not as flexible.
		 * @param roles The roles to populate. Note that if the role does not start with
		 * {@link #rolePrefix(String)} it will automatically be appended. This means by
		 * default {@code roles("ROLE_USER")} and {@code roles("USER")} are equivalent.
		 * @return
		 * @see #authorities(GrantedAuthority...)
		 * @see #rolePrefix(String)
		 */
		public UserRequestPostProcessor roles(String... roles) {
			List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(roles.length);
			for(String role : roles) {
				if(rolePrefix == null || role.startsWith(rolePrefix)) {
					authorities.add(new SimpleGrantedAuthority(role));
				} else {
					authorities.add(new SimpleGrantedAuthority(rolePrefix + role));
				}
			}
			return this;
		}

		/**
		 * Populates the user's {@link GrantedAuthority}'s.
		 * @param authorities
		 * @return
		 * @see #roles(String...)
		 */
		public UserRequestPostProcessor authorities(GrantedAuthority... authorities) {
			this.authorities = Arrays.asList(authorities);
			return this;
		}
	}

	public final static class UserDetailsRequestPostProcessor extends AbstractSecurityContextRequestPostProcessor {
		private final String username;
		private String userDetailsServiceBeanId;

		private UserDetailsRequestPostProcessor(String username) {
			this.username = username;
		}

		public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
			UsernamePasswordAuthenticationToken authentication = authentication(request.getServletContext());
			save(authentication,request);
			return request;
		}

		/**
		 * Use this method to specify the bean id of the {@link UserDetailsService} to
		 * use to look up the {@link UserDetails}.
		 *
		 * <p>By default a lookup of {@link UserDetailsService} is performed by type. This
		 * can be problematic if multiple {@link UserDetailsService} beans are declared.
		 *
		 * @param userDetailsServiceBeanId
		 * @return
		 */
		public UserDetailsRequestPostProcessor userDetailsServiceBeanId(String userDetailsServiceBeanId) {
			this.userDetailsServiceBeanId = userDetailsServiceBeanId;
			return this;
		}

		private UsernamePasswordAuthenticationToken authentication(ServletContext servletContext) {
			ApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
			UserDetailsService  userDetailsService = userDetailsService(context);
			UserDetails userDetails = userDetailsService.loadUserByUsername(username);
			return new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
		}

		private UserDetailsService userDetailsService(ApplicationContext context) {
			if(userDetailsServiceBeanId == null) {
				return context.getBean(UserDetailsService.class);
			}
			return context.getBean(userDetailsServiceBeanId, UserDetailsService.class);
		}
	}

	private static abstract class AbstractSecurityContextRequestPostProcessor implements RequestPostProcessor {
		private SecurityContextRepository repository = new HttpSessionSecurityContextRepository();

		final void save(Authentication authentication, HttpServletRequest request) {
			SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
			securityContext.setAuthentication(authentication);
			save(securityContext, request);
		}

		final void save(SecurityContext securityContext, HttpServletRequest request) {
			HttpServletResponse response = new MockHttpServletResponse();

			HttpRequestResponseHolder requestResponseHolder = new HttpRequestResponseHolder(request, response);
			repository.loadContext(requestResponseHolder);

			request = requestResponseHolder.getRequest();
			response = requestResponseHolder.getResponse();

			repository.saveContext(securityContext, request, response);
		}
	}

	private SecurityRequestPostProcessors() {}
}
