package com.bazaarvoice.dropwizard.webjars;

import com.google.common.base.Throwables;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class WebJarServletCustomPathTest {

    public static final String CUSTOM_PATH = "/custom/";
    private final ServletTester servletTester = new ServletTester();
    private final WebJarServlet webJarServlet = new WebJarServlet(null,
                                                                  Arrays.asList(WebJarServlet.DEFAULT_MAVEN_GROUPS), CUSTOM_PATH);

    @Before
    public void setup() throws Exception {
        servletTester.addServlet(new ServletHolder(webJarServlet), CUSTOM_PATH + "*");
        servletTester.start();
    }

    @After
    public void teardown() throws Exception {
        servletTester.stop();
        TestWebJarServlet.resetMavenGroups();
    }

    @Test
    public void testBootstrapAtCustomPath() throws Exception {
        HttpTester.Response response = get("bootstrap/css/bootstrap.css");
        assertEquals(200, response.getStatus());

    }

    private HttpTester.Request request(String url) {
        HttpTester.Request request = HttpTester.newRequest();
        request.setMethod("GET");
        request.setVersion("HTTP/1.0");
        request.setURI(CUSTOM_PATH + url);
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
}
