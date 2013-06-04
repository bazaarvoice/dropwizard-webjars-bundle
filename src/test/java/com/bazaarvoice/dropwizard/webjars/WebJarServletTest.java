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

public class WebJarServletTest {
    private final ServletTester servletTester = new ServletTester();

    @Before
    public void setup() throws Exception {
        servletTester.addServlet(TestWebJarServlet.class, TestWebJarServlet.URL_PREFIX + "*");
        servletTester.start();
    }

    @After
    public void teardown() throws Exception {
        servletTester.stop();
        TestWebJarServlet.resetMavenGroups();
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
    public void testNonStandardGroupWebjar() throws Exception {
        setMavenGroups("org.webjars", "com.bazaarvoice");

        HttpTester response = get("test-webjar/hello.txt");
        assertEquals(200, response.getStatus());
        assertEquals("Hello World!", response.getContent());
    }

    @Test
    public void testCorrectETag() {
        String eTag = get("bootstrap/css/bootstrap.css").getHeader(ETAG);

        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addHeader(IF_NONE_MATCH, eTag);

        HttpTester response = get(request);
        assertEquals(304, response.getStatus());
    }

    @Test
    public void testWildcardETag() {
        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addHeader(IF_NONE_MATCH, "*");

        HttpTester response = get(request);
        assertEquals(304, response.getStatus());
        assertNotNull(response.getHeader(ETAG));
    }

    @Test
    public void testGzipETag() {
        String eTag = get("bootstrap/css/bootstrap.css").getHeader(ETAG);
        eTag = eTag.substring(0, eTag.length() - 1) + "-gzip" + '"';

        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addHeader(IF_NONE_MATCH, eTag);

        HttpTester response = get(request);
        assertEquals(304, response.getStatus());
    }

    @Test
    public void testWrongETag() {
        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addHeader(IF_NONE_MATCH, '"' + "wrong-etag" + '"');

        HttpTester response = get(request);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getHeader(ETAG));
    }

    @Test
    public void testUnquotedETag() {
        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addHeader(IF_NONE_MATCH, "not-the-right-etag");

        HttpTester response = get(request);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getHeader(ETAG));
    }

    @Test
    public void testCorrectIfModifiedSince() {
        long lastModified = get("bootstrap/css/bootstrap.css").getDateHeader(LAST_MODIFIED);

        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addDateHeader(IF_MODIFIED_SINCE, lastModified);

        HttpTester response = get(request);
        assertEquals(304, response.getStatus());
    }

    @Test
    public void testPastIfModifiedSince() {
        long lastModified = get("bootstrap/css/bootstrap.css").getDateHeader(LAST_MODIFIED);

        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addDateHeader(IF_MODIFIED_SINCE, lastModified - 1);

        HttpTester response = get(request);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testFutureIfModifiedSince() {
        long lastModified = get("bootstrap/css/bootstrap.css").getDateHeader(LAST_MODIFIED);

        HttpTester request = request("bootstrap/css/bootstrap.css");
        request.addDateHeader(IF_MODIFIED_SINCE, lastModified + 1);

        HttpTester response = get(request);
        assertEquals(304, response.getStatus());
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

    private void setMavenGroups(String... groups) {
        try {
            servletTester.stop();
            servletTester.join();

            TestWebJarServlet.setMavenGroups(groups);

            servletTester.start();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
