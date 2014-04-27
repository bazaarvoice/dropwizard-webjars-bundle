package com.bazaarvoice.dropwizard.webjars;

import io.dropwizard.setup.Environment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.ServletRegistration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WebJarBundleTest {
    private final Environment environment = mock(Environment.class, RETURNS_DEEP_STUBS);

    @Test
    public void testRegistersAtWebjarsPath() {
        new WebJarBundle().run(environment);

        ServletRegistration.Dynamic dynamic = environment.servlets().addServlet(eq("webjars"), notNull(WebJarServlet.class));
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(dynamic).addMapping(pathCaptor.capture());

        assertEquals("/webjars/*", pathCaptor.getValue());
    }
}
