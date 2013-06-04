package com.bazaarvoice.dropwizard.webjars;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.yammer.dropwizard.ConfiguredBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.GzipConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class WebJarBundle implements ConfiguredBundle<Configuration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebJarBundle.class);

    private CacheBuilder cacheBuilder = null;
    private List<String> packages = Lists.newArrayList(WebJarServlet.DEFAULT_WEBJAR_PACKAGES);

    public WebJarBundle() {
    }

    public WebJarBundle(CacheBuilder builder) {
        cacheBuilder = builder;
    }

    public WebJarBundle(String... additionalPackages) {
        Collections.addAll(packages, additionalPackages);
    }

    public WebJarBundle(CacheBuilder builder, String... additionalPackages) {
        cacheBuilder = builder;
        Collections.addAll(packages, additionalPackages);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run(Configuration config, Environment environment) {
        GzipConfiguration gzip = config.getHttpConfiguration().getGzipConfiguration();
        if (gzip.isEnabled()) {
            LOGGER.warn("Disabling gzip as it's incompatible with the WebJarBundle.");
            gzip.setEnabled(false);
        }

        WebJarServlet servlet = new WebJarServlet(cacheBuilder, packages);
        environment.addServlet(servlet, WebJarServlet.URL_PREFIX + "*");
    }
}
