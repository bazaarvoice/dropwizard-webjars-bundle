package com.bazaarvoice.dropwizard.webjars;

import com.google.common.base.Throwables;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.servlet.ServletTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

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
        HttpTester.Response response = get("bootstrap/css/bootstrap.css");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testBootstrapResourceThatDoesNotExist() {
        HttpTester.Response response = get("bootstrap/css/bootstrap.resource.that.does.not.exist");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testWebjarThatDoesNotExist() {
        HttpTester.Response response = get("webjar-that-does-not-exist/css/app.css");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testNonStandardGroupWebjar() throws Exception {
        setMavenGroups("org.webjars", "com.bazaarvoice");

        HttpTester.Response response = get("test-webjar/hello.txt");
        assertEquals(200, response.getStatus());
        assertEquals("Hello World!", response.getContent());
    }

    @Test
    public void testCorrectETag() {
        String eTag = get("bootstrap/css/bootstrap.css").get(ETAG);

        HttpTester.Request request = request("bootstrap/css/bootstrap.css");
        request.setHeader(IF_NONE_MATCH, eTag);

        HttpTester.Response response = get(request);
        assertEquals(304, response.getStatus());
    }

    @Test
    public void testWildcardETag() {
        HttpTester.Request request = request("bootstrap/css/bootstrap.css");
        request.setHeader(IF_NONE_MATCH, "*");

        HttpTester.Response response = get(request);
        assertEquals(304, response.getStatus());
        assertNotNull(response.get(ETAG));
    }

    @Test
    public void testGzipETag() {
        String eTag = get("bootstrap/css/bootstrap.css").get(ETAG);
        eTag = eTag.substring(0, eTag.length() - 1) + "-gzip" + '"';

        HttpTester.Request request = request("bootstrap/css/bootstrap.css");
        request.setHeader(IF_NONE_MATCH, eTag);

        HttpTester.Response response = get(request);
        assertEquals(304, response.getStatus());
    }

    @Test
    public void testWrongETag() {
        HttpTester.Request request = request("bootstrap/css/bootstrap.css");
        request.setHeader(IF_NONE_MATCH, '"' + "wrong-etag" + '"');

        HttpTester.Response response = get(request);
        assertEquals(200, response.getStatus());
        assertNotNull(response.get(ETAG));
    }

    @Test
    public void testUnquotedETag() {
        HttpTester.Request request = request("bootstrap/css/bootstrap.css");
        request.setHeader(IF_NONE_MATCH, "not-the-right-etag");

        HttpTester.Response response = get(request);
        assertEquals(200, response.getStatus());
        assertNotNull(response.get(ETAG));
    }

    @Test
    public void testCorrectIfModifiedSince() {
        long lastModified = get("bootstrap/css/bootstrap.css").getDateField(LAST_MODIFIED);

        HttpTester.Request request = request("bootstrap/css/bootstrap.css");
        request.addDateField(IF_MODIFIED_SINCE, lastModified);

        HttpTester.Response response = get(request);
        assertEquals(304, response.getStatus());
    }

    @Test
    public void testPastIfModifiedSince() {
        long lastModified = get("bootstrap/css/bootstrap.css").getDateField(LAST_MODIFIED);

        HttpTester.Request request = request("bootstrap/css/bootstrap.css");
        request.addDateField(IF_MODIFIED_SINCE, lastModified - 1);

        HttpTester.Response response = get(request);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testFutureIfModifiedSince() {
        long lastModified = get("bootstrap/css/bootstrap.css").getDateField(LAST_MODIFIED);

        HttpTester.Request request = request("bootstrap/css/bootstrap.css");
        request.addDateField(IF_MODIFIED_SINCE, lastModified + 1);

        HttpTester.Response response = get(request);
        assertEquals(304, response.getStatus());
    }

    private HttpTester.Request request(String url) {
        HttpTester.Request request = HttpTester.newRequest();
        request.setMethod("GET");
        request.setVersion("HTTP/1.0");
        request.setURI(WebJarServlet.URL_PREFIX + url);
        return request;
    }

    private HttpTester.Response get(String url) {
        HttpTester.Request request = request(url);
        return get(request);
    }

    private HttpTester.Response get(HttpTester.Request request) {
        HttpTester.Response response;
        try {
            ByteBuffer raw = request.generate();
            ByteBuffer responses = servletTester.getResponses(raw);
            response = HttpTester.parseResponse(responses);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return response;
    }

    private void setMavenGroups(String... groups) {
        try {
            servletTester.stop();

            TestWebJarServlet.setMavenGroups(groups);

            servletTester.start();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
