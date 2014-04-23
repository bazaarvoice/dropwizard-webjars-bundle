package com.bazaarvoice.dropwizard.webjars;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Collections;
import java.util.List;

public class WebJarBundle implements Bundle {
    private CacheBuilder cacheBuilder = null;
    private List<String> packages = Lists.newArrayList(WebJarServlet.DEFAULT_MAVEN_GROUPS);

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

    @Override
    public void run(Environment environment) {
        WebJarServlet servlet = new WebJarServlet(cacheBuilder, packages);
        environment.servlets().addServlet("webjars", servlet).addMapping(WebJarServlet.URL_PREFIX + "*");
    }
}
