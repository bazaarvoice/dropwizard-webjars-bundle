package com.bazaarvoice.dropwizard.webjars;

import io.dropwizard.setup.Environment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WebJarBundleTest {
    private final Environment environment = mock(Environment.class, RETURNS_DEEP_STUBS);

    @Test
    public void testRegistersAtWebjarsPath() {
        new WebJarBundle().run(environment);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(environment.servlets().addServlet(Matchers.eq("webjars"), Matchers.notNull(WebJarServlet.class))).addMapping(pathCaptor.capture());

        assertEquals("/webjars/*", pathCaptor.getValue());
    }
}
