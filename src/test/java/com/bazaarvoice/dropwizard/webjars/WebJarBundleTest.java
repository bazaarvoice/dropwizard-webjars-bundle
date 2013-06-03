package com.bazaarvoice.dropwizard.webjars;

import com.yammer.dropwizard.config.Environment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WebJarBundleTest {
    private final Environment environment = mock(Environment.class);

    private WebJarServlet servlet;
    private String path;

    @Test
    public void testRegistersAtWebjarsPath() {
        runBundle(new WebJarBundle());

        assertNotNull(servlet);
        assertEquals("/webjars/*", path);
    }

    private void runBundle(WebJarBundle bundle) {
        bundle.run(environment);

        ArgumentCaptor<WebJarServlet> servletCaptor = ArgumentCaptor.forClass(WebJarServlet.class);
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);

        verify(environment).addServlet(servletCaptor.capture(), pathCaptor.capture());

        servlet = servletCaptor.getValue();
        path = pathCaptor.getValue();
    }
}
