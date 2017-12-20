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

package de.qaware.web.filter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import static de.qaware.web.filter.ForwardedHeaderConstants.ENABLE_RELATIVE_REDIRECTS_INIT_PARAM;
import static de.qaware.web.filter.ForwardedHeaderConstants.REMOVE_ONLY_INIT_PARAM;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ForwardedHeaderFilter}.
 *
 * @author Rossen Stoyanchev
 * @author Eddú Meléndez
 * @author Rob Winch
 */
public class ForwardedHeaderFilterTests {

	private static final String X_FORWARDED_PROTO = "x-forwarded-proto";  // SPR-14372 (case insensitive)
	private static final String X_FORWARDED_HOST = "x-forwarded-host";
	private static final String X_FORWARDED_PORT = "x-forwarded-port";
	private static final String X_FORWARDED_PREFIX = "x-forwarded-prefix";
	public static final String UNIT_TEST_FORWARED_FILTER = "unitTestForwaredFilter";


	private ForwardedHeaderFilter filter;

	private MockHttpServletRequest request;

	private MockFilterChain filterChain;


	@Before
	@SuppressWarnings("serial")
	public void setup() throws Exception {
		this.filter= new ForwardedHeaderFilter();
		initFilter(UNIT_TEST_FORWARED_FILTER,null);
		this.request = new MockHttpServletRequest();
		this.request.setScheme("http");
		this.request.setServerName("localhost");
		this.request.setServerPort(80);
		this.filterChain = new MockFilterChain(new HttpServlet() {});
	}


	private ForwardedHeaderFilter initFilter(String filterName, Map<String,String> params) throws ServletException {
		MockFilterConfig config= new MockFilterConfig(filterName);
		if(params!=null){
			params.forEach(config::addInitParameter);
		}
		filter.init(config);
		return filter;
	}

	@Test
	public void contextPathEmpty() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "");
		assertEquals("", filterAndGetContextPath());
	}

	@Test
	public void contextPathWithTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/foo/bar/");
		assertEquals("/foo/bar", filterAndGetContextPath());
	}

	@Test
	public void contextPathWithTrailingSlashes() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/foo/bar/baz///");
		assertEquals("/foo/bar/baz", filterAndGetContextPath());
	}

	@Test
	public void contextPathWithForwardedPrefix() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix");
		this.request.setContextPath("/mvc-showcase");

		String actual = filterAndGetContextPath();
		assertEquals("/prefix", actual);
	}

	@Test
	public void contextPathWithForwardedPrefixTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix/");
		this.request.setContextPath("/mvc-showcase");

		String actual = filterAndGetContextPath();
		assertEquals("/prefix", actual);
	}

	@Test
	public void contextPathPreserveEncoding() throws Exception {
		this.request.setContextPath("/app%20");
		this.request.setRequestURI("/app%20/path/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("/app%20", actual.getContextPath());
		assertEquals("/app%20/path/", actual.getRequestURI());
		assertEquals("http://localhost/app%20/path/", actual.getRequestURL().toString());
	}

	@Test
	public void requestUri() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("", actual.getContextPath());
		assertEquals("/path", actual.getRequestURI());
	}

	@Test
	public void requestUriWithTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("", actual.getContextPath());
		assertEquals("/path/", actual.getRequestURI());
	}

	@Test
	public void requestUriPreserveEncoding() throws Exception {
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path%20with%20spaces/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("/app", actual.getContextPath());
		assertEquals("/app/path%20with%20spaces/", actual.getRequestURI());
		assertEquals("http://localhost/app/path%20with%20spaces/", actual.getRequestURL().toString());
	}

	@Test
	public void requestUriEqualsContextPath() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("", actual.getContextPath());
		assertEquals("/", actual.getRequestURI());
	}

	@Test
	public void requestUriRootUrl() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("", actual.getContextPath());
		assertEquals("/", actual.getRequestURI());
	}

	@Test
	public void requestUriPreserveSemicolonContent() throws Exception {
		this.request.setContextPath("");
		this.request.setRequestURI("/path;a=b/with/semicolon");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("", actual.getContextPath());
		assertEquals("/path;a=b/with/semicolon", actual.getRequestURI());
		assertEquals("http://localhost/path;a=b/with/semicolon", actual.getRequestURL().toString());
	}

	@Test
	public void caseInsensitiveForwardedPrefix() throws Exception {
		this.request = new MockHttpServletRequest() {
			// Make it case-sensitive (SPR-14372)
			@Override
			public String getHeader(String header) {
				Enumeration<String> names = getHeaderNames();
				while (names.hasMoreElements()) {
					String name = names.nextElement();
					if (name.equals(header)) {
						return super.getHeader(header);
					}
				}
				return null;
			}
		};
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix");
		this.request.setRequestURI("/path");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("/prefix/path", actual.getRequestURI());
	}

	@Test
	public void shouldFilter() throws Exception {
		testShouldFilter("Forwarded");
		testShouldFilter(X_FORWARDED_HOST);
		testShouldFilter(X_FORWARDED_PORT);
		testShouldFilter(X_FORWARDED_PROTO);
	}

	@Test
	public void shouldNotFilter() throws Exception {
		assertTrue(this.filter.shouldNotFilter(new MockHttpServletRequest()));
	}

	@Test
	public void forwardedRequest() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.addHeader("foo", "bar");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		assertEquals("https://84.198.58.199/mvc-showcase", actual.getRequestURL().toString());
		assertEquals("https", actual.getScheme());
		assertEquals("84.198.58.199", actual.getServerName());
		assertEquals(443, actual.getServerPort());
		assertTrue(actual.isSecure());

		assertNull(actual.getHeader(X_FORWARDED_PROTO));
		assertNull(actual.getHeader(X_FORWARDED_HOST));
		assertNull(actual.getHeader(X_FORWARDED_PORT));
		assertEquals("bar", actual.getHeader("foo"));
	}

	@Test
	public void forwardedRequestInRemoveOnlyMode() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.addHeader("foo", "bar");

		this.filter =initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(REMOVE_ONLY_INIT_PARAM,"true"));
		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		assertEquals("http://localhost/mvc-showcase", actual.getRequestURL().toString());
		assertEquals("http", actual.getScheme());
		assertEquals("localhost", actual.getServerName());
		assertEquals(80, actual.getServerPort());
		assertFalse(actual.isSecure());

		assertNull(actual.getHeader(X_FORWARDED_PROTO));
		assertNull(actual.getHeader(X_FORWARDED_HOST));
		assertNull(actual.getHeader(X_FORWARDED_PORT));
		assertEquals("bar", actual.getHeader("foo"));
	}

	@Test
	public void requestUriWithForwardedPrefix() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		assertEquals("http://localhost/prefix/mvc-showcase", actual.getRequestURL().toString());
	}

	@Test
	public void requestUriWithForwardedPrefixTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix/");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		assertEquals("http://localhost/prefix/mvc-showcase", actual.getRequestURL().toString());
	}
	
	@Test
	public void requestURLNewStringBuffer() throws Exception { 
		this.request.addHeader(X_FORWARDED_PREFIX, "/prefix/");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		actual.getRequestURL().append("?key=value");
		assertEquals("http://localhost/prefix/mvc-showcase", actual.getRequestURL().toString());
	}

	@Test
	public void sendRedirectWithAbsolutePath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		String redirectedUrl = sendRedirect("/foo/bar");
		assertEquals("https://example.com/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithContextPath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.setContextPath("/context");

		String redirectedUrl = sendRedirect("/context/foo/bar");
		assertEquals("https://example.com/context/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithRelativePath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.setRequestURI("/parent/");

		String redirectedUrl = sendRedirect("foo/bar");
		assertEquals("https://example.com/parent/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithFileInPathAndRelativeRedirect() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.setRequestURI("/context/a");

		String redirectedUrl = sendRedirect("foo/bar");
		assertEquals("https://example.com/context/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithRelativePathIgnoresFile() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");
		this.request.setRequestURI("/parent");

		String redirectedUrl = sendRedirect("foo/bar");
		assertEquals("https://example.com/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithLocationDotDotPath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		String redirectedUrl = sendRedirect("parent/../foo/bar");
		assertEquals("https://example.com/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithLocationHasScheme() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		String location = "http://other.info/foo/bar";
		String redirectedUrl = sendRedirect(location);
		assertEquals(location, redirectedUrl);
	}

	@Test
	public void sendRedirectWithLocationSlashSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		String location = "//other.info/foo/bar";
		String redirectedUrl = sendRedirect(location);
		assertEquals("https:" + location, redirectedUrl);
	}

	@Test
	public void sendRedirectWithLocationSlashSlashParentDotDot() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		String location = "//other.info/parent/../foo/bar";
		String redirectedUrl = sendRedirect(location);
		assertEquals("https:" + location, redirectedUrl);
	}

	@Test
	public void sendRedirectWithNoXForwardedAndAbsolutePath() throws Exception {
		String redirectedUrl = sendRedirect("/foo/bar");
		assertEquals("/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithNoXForwardedAndDotDotPath() throws Exception {
		String redirectedUrl = sendRedirect("../foo/bar");
		assertEquals("../foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWhenRequestOnlyAndXForwardedThenUsesRelativeRedirects() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO, "https");
		this.request.addHeader(X_FORWARDED_HOST, "example.com");
		this.request.addHeader(X_FORWARDED_PORT, "443");

		initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(ENABLE_RELATIVE_REDIRECTS_INIT_PARAM,"true"));
		filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);

		String location = sendRedirect("/a");

		assertEquals("/a", location);
	}

	@Test
	public void sendRedirectWhenRequestOnlyAndNoXForwardedThenUsesRelativeRedirects() throws Exception {
		initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(ENABLE_RELATIVE_REDIRECTS_INIT_PARAM,"true"));

		String location = sendRedirect("/a");

		assertEquals("/a", location);
	}

	private String sendRedirect(final String location) throws ServletException, IOException {
		Filter triggerRedirect= new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {
				response.sendRedirect(location);
			}
		};
		triggerRedirect.init(new MockFilterConfig("triggerRedirectFilter"));

		MockHttpServletResponse response = doWithFiltersAndGetResponse(this.filter,triggerRedirect) ;
		return response.getRedirectedUrl();
	}

	@SuppressWarnings("serial")
	private MockHttpServletResponse doWithFiltersAndGetResponse(Filter... filters) throws ServletException, IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = new MockFilterChain(new HttpServlet() {}, filters);
		filterChain.doFilter(request, response);
		return response;
	}

	private String filterAndGetContextPath() throws ServletException, IOException {
		return filterAndGetWrappedRequest().getContextPath();
	}

	private HttpServletRequest filterAndGetWrappedRequest() throws ServletException, IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilterInternal(this.request, response, this.filterChain);
		return (HttpServletRequest) this.filterChain.getRequest();
	}

	private void testShouldFilter(String headerName) throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(headerName, "1");
		assertFalse(this.filter.shouldNotFilter(request));
	}

}
