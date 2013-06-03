package com.bazaarvoice.dropwizard.webjars;

import com.google.common.base.Throwables;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.net.HttpHeaders.ETAG;
import static com.google.common.net.HttpHeaders.IF_MODIFIED_SINCE;
import static com.google.common.net.HttpHeaders.IF_NONE_MATCH;
import static com.google.common.net.HttpHeaders.LAST_MODIFIED;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WebJarServletTest {
    private final String[] DEFAULT_WEBJAR_PACKAGES = WebJarServlet.DEFAULT_WEBJAR_PACKAGES;
    private final ServletTester servletTester = new ServletTester();

    @Before
    public void setup() throws Exception {
        servletTester.addServlet(WebJarServlet.class, WebJarServlet.URL_PREFIX + "*");
        servletTester.start();
    }

    @After
    public void teardown() throws Exception {
        servletTester.stop();

        // Make sure we always restore the packages back to normal after every test.
        WebJarServlet.DEFAULT_WEBJAR_PACKAGES = DEFAULT_WEBJAR_PACKAGES;
    }

    @Test
    public void testBootstrap() {
        HttpTester response = get("bootstrap/css/bootstrap.css");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testBootstrapResourceThatDoesNotExist() {
        HttpTester response = get("bootstrap/css/bootstrap.resource.that.does.not.exist");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testWebjarThatDoesNotExist() {
        HttpTester response = get("webjar-that-does-not-exist/css/app.css");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testNonStandardWebjar() throws Exception {
        HttpTester response1 = get("test-webjar/hello.txt");
        assertEquals(404, response1.getStatus());

        // Restart the servlet tester but this time include the com.bazaarvoice package when looking for webjars.
        servletTester.stop();
        servletTester.join();
        WebJarServlet.DEFAULT_WEBJAR_PACKAGES = new String[] {"org.webjars", "com.bazaarvoice"};
        servletTester.start();

        HttpTester response2 = get("test-webjar/hello.txt");
        assertEquals(200, response2.getStatus());
        assertEquals("Hello World!", response2.getContent());
    }

    @Test
    public void testCorrectEtag() {
        HttpTester response1 = get("bootstrap/css/bootstrap.css");
        assertEquals(200, response1.getStatus());
        assertNotNull(response1.getHeader(ETAG));

        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addHeader(IF_NONE_MATCH, response1.getHeader(ETAG));

        HttpTester response2 = get(request);
        assertEquals(304, response2.getStatus());
    }

    @Test
    public void testWildcardEtag() {
        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addHeader(IF_NONE_MATCH, "*");

        HttpTester response = get(request);
        assertEquals(304, response.getStatus());
        assertNotNull(response.getHeader(ETAG));
    }

    @Test
    public void testWrongEtag() {
        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addHeader(IF_NONE_MATCH, "\"not-the-right-etag\"");

        HttpTester response = get(request);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getHeader(ETAG));
    }

    @Test
    public void testMalformedEtag() {
        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addHeader(IF_NONE_MATCH, "not-the-right-etag");  // Etags should be quoted

        HttpTester response = get(request);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getHeader(ETAG));
    }

    @Test
    public void testCorrectIfModifiedSince() {
        HttpTester response1 = get("bootstrap/css/bootstrap.css");
        assertEquals(200, response1.getStatus());
        assertTrue(response1.getDateHeader(LAST_MODIFIED) > 0);

        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addDateHeader(IF_MODIFIED_SINCE, response1.getDateHeader(LAST_MODIFIED));

        HttpTester response2 = get(request);
        assertEquals(304, response2.getStatus());
    }

    @Test
    public void testPastIfModifiedSince() {
        HttpTester response1 = get("bootstrap/css/bootstrap.css");
        assertEquals(200, response1.getStatus());
        assertTrue(response1.getDateHeader(LAST_MODIFIED) > 0);

        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addDateHeader(IF_MODIFIED_SINCE, response1.getDateHeader(LAST_MODIFIED)-1);

        HttpTester response2 = get(request);
        assertEquals(200, response2.getStatus());
    }

    @Test
    public void testFutureIfModifiedSince() {
        HttpTester response1 = get("bootstrap/css/bootstrap.css");
        assertEquals(200, response1.getStatus());
        assertTrue(response1.getDateHeader(LAST_MODIFIED) > 0);

        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addDateHeader(IF_MODIFIED_SINCE, response1.getDateHeader(LAST_MODIFIED)+1);

        HttpTester response2 = get(request);
        assertEquals(304, response2.getStatus());
    }

    private HttpTester request(String url) {
        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.setVersion("HTTP/1.0");
        request.setURI(WebJarServlet.URL_PREFIX + url);
        return request;
    }

    private HttpTester get(String url) {
        HttpTester request = request(url);
        return get(request);
    }

    private HttpTester get(HttpTester request) {
        HttpTester response = new HttpTester();
        try {
            String raw = request.generate();
            String responses = servletTester.getResponses(raw);
            response.parse(responses);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return response;
    }
}
