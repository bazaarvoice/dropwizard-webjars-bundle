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
    private String urlPrefix;

    public WebJarBundle() {

        this(null, null, null);
    }

    public WebJarBundle(final String urlPrefix) {
        this(urlPrefix, null, null);
    }

    public WebJarBundle(CacheBuilder builder) {

        this(null, builder, null);
        cacheBuilder = builder;
    }

    public WebJarBundle(final String urlPrefix, CacheBuilder builder) {
        this(urlPrefix, builder, null);
    }

    public WebJarBundle(String... additionalPackages) {
        this(null, null, additionalPackages);
    }

    public WebJarBundle(final String urlPrefix, String... additionalPackages) {
        this(urlPrefix, null, additionalPackages);
    }

    public WebJarBundle(CacheBuilder builder, String... additionalPackages) {
        this(null, builder, additionalPackages);
    }

    public WebJarBundle(final String urlPrefix, CacheBuilder builder, String... additionalPackages) {
        this.urlPrefix = (urlPrefix == null? WebJarServlet.DEFAULT_URL_PREFIX : urlPrefix);
        cacheBuilder = builder;
        if(additionalPackages != null) {
            Collections.addAll(packages, additionalPackages);
        }

    }

    private String normalizedUrlPrefix() {
        final StringBuilder pathBuilder = new StringBuilder();
        if(! urlPrefix.startsWith("/")) {
            pathBuilder.append('/');
        }
        pathBuilder.append(urlPrefix);
        if(! urlPrefix.endsWith("/")) {
            pathBuilder.append('/');
        }
        return pathBuilder.toString();
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(Environment environment) {
        WebJarServlet servlet = new WebJarServlet(cacheBuilder, packages, normalizedUrlPrefix());
        environment.servlets().addServlet("webjars", servlet).addMapping(normalizedUrlPrefix() + "*");
    }
}
