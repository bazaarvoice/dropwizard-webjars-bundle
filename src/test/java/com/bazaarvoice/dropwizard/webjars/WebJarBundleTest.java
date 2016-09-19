package com.bazaarvoice.dropwizard.webjars;

import io.dropwizard.setup.Environment;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.servlet.ServletRegistration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

public class WebJarBundleTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Environment environment;

    @Captor
    private ArgumentCaptor<String> pathCaptor;

    @Test
    public void testRegistersAtWebjarsPath() {
        new WebJarBundle().run(environment);

        ServletRegistration.Dynamic dynamic = environment.servlets().addServlet(eq("webjars"), notNull(WebJarServlet.class));
        verify(dynamic).addMapping(pathCaptor.capture());

        assertEquals("/webjars/*", pathCaptor.getValue());
    }

    @Test
    public void testRegistersAtCustomPath() {
        final String path = "path";
        new WebJarBundle(path).run(environment);

        ServletRegistration.Dynamic dynamic = environment.servlets().addServlet(eq("webjars"), notNull(WebJarServlet.class));
        verify(dynamic).addMapping(pathCaptor.capture());

        assertEquals("/" + path + "/*", pathCaptor.getValue());
    }
}
