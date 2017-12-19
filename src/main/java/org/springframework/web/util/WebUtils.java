/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.util;

import javax.servlet.ServletRequest;

/**
 * Miscellaneous utilities for web applications.
 * Used by various framework classes.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 */
public abstract class WebUtils {

	/**
	 * Standard Servlet 2.3+ spec request attributes for include URI and paths.
	 * <p>If included via a RequestDispatcher, the current resource will see the
	 * originating request. Its own URI and paths are exposed as request attributes.
	 */
	public static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri";
	public static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.include.context_path";

	public static final String ERROR_REQUEST_URI_ATTRIBUTE = "javax.servlet.error.request_uri";


	/**
	 * Default character encoding to use when {@code request.getCharacterEncoding}
	 * returns {@code null}, according to the Servlet spec.
	 * @see ServletRequest#getCharacterEncoding
	 */
	public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";


}
