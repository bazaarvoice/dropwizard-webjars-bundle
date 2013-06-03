package com.bazaarvoice.dropwizard.webjars;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.yammer.dropwizard.Bundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import java.util.Collections;
import java.util.List;

public class WebJarBundle implements Bundle {
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
    public void run(Environment environment) {
        WebJarServlet servlet = new WebJarServlet(cacheBuilder, packages);
        environment.addServlet(servlet, WebJarServlet.URL_PREFIX + "*");
    }
}
