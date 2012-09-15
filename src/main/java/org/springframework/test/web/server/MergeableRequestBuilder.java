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
package org.springframework.test.web.server;

/**
 * A {@link RequestBuilder} whose properties can be merged those of another.
 *
 * @author Rossen Stoyanchev
 */
public interface MergeableRequestBuilder extends RequestBuilder {

	/**
	 * Merge the properties of the supplied {@code RequestBuilder} picking up
	 * its properties only if the same properties in "this"
	 * {@code RequestBuilder} are not already set.
	 *
	 * @param other the {@code RequestBuilder} to merge with
	 */
	void merge(RequestBuilder other);

}
