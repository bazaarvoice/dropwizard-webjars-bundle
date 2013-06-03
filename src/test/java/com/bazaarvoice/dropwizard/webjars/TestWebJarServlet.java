package com.bazaarvoice.dropwizard.webjars;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

public class TestWebJarServlet extends WebJarServlet {
    private static String[] MAVEN_GROUPS = WebJarServlet.DEFAULT_MAVEN_GROUPS;

    public static void setMavenGroups(String... groups) {
        MAVEN_GROUPS = checkNotNull(groups);
    }

    public static void resetMavenGroups() {
        MAVEN_GROUPS = WebJarServlet.DEFAULT_MAVEN_GROUPS;
    }

    public TestWebJarServlet() {
        super(null, ImmutableList.copyOf(MAVEN_GROUPS));
    }
}
