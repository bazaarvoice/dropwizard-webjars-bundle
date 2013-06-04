package com.bazaarvoice.dropwizard.webjars;

import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.GzipConfiguration;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebJarBundleTest {
    private final Configuration configuration = mock(Configuration.class, RETURNS_DEEP_STUBS);
    private final Environment environment = mock(Environment.class);

    private WebJarServlet servlet;
    private String path;

    @Test
    public void testRegistersAtWebjarsPath() {
        runBundle(new WebJarBundle());

        assertNotNull(servlet);
        assertEquals("/webjars/*", path);
    }

    @Test
    public void testDisablesGzip() {
        GzipConfiguration gzip = configuration.getHttpConfiguration().getGzipConfiguration();
        when(gzip.isEnabled()).thenReturn(true);

        runBundle(new WebJarBundle());
        verify(gzip).setEnabled(false);
    }

    private void runBundle(WebJarBundle bundle) {
        bundle.run(configuration, environment);

        ArgumentCaptor<WebJarServlet> servletCaptor = ArgumentCaptor.forClass(WebJarServlet.class);
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);

        verify(environment).addServlet(servletCaptor.capture(), pathCaptor.capture());

        servlet = servletCaptor.getValue();
        path = pathCaptor.getValue();
    }
}
