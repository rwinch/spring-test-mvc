/*
 * Copyright 2011-2012 the original author or authors.
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

package org.springframework.test.web.server;


/**
 * A contract for defining expected actions on the results of an executed request.
 *
 * <p>See static factory methods in
 * {@code org.springframework.test.web.server.result.MockMvcResultMatchers} and
 * {@code org.springframework.test.web.server.result.MockMvcResultHandlers}.
 *
 * @author Rossen Stoyanchev
 */
public interface ExpectedResultActions {

	/**
	 * Provide an expectation to be matched to every request. For example:
	 * <pre>
	 * // Assuming static import of MockMvcResultMatchers.*
	 *
	 * mockMvc.alwaysPerform(get("/").accept(MediaType.APPLICATION_JSON))
	 *   .andAlwaysExpect(status.isOk())
	 *   .andAlwaysExpect(content().mimeType(MediaType.APPLICATION_JSON));
	 * </pre>
	 */
	ExpectedResultActions andAlwaysExpect(ResultMatcher matcher);

	/**
	 * Provide a general action. For example:
	 * <pre>
	 * // Assuming static imports of MockMvcResultHandlers.* and MockMvcResultMatchers.*
	 *
	 * mockMvc.alwaysPerform(get("/").accept(MediaType.APPLICATION_JSON))
	 *   .andAlwaysDo(print())
	 *   .andAlwaysExpect(status.isOk())
	 *   .andAlwaysExpect(content().mimeType(MediaType.APPLICATION_JSON));
	 * </pre>
	 */
	ExpectedResultActions andAlwaysDo(ResultHandler handler);

}