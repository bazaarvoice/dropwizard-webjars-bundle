package com.bazaarvoice.dropwizard.webjars;

import com.google.common.cache.CacheLoader;
import com.google.common.io.Closer;
import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Determines the version of the webjar that's in the classpath for a particular library.
 * <p/>
 * The version of a webjar can be determined by looking at the <code>pom.properties</code> file located at
 * {@code META-INF/maven/<group>/<library>/pom.properties}.
 * <p/>
 * Where {@code <group>} is the name of the maven group the webjar artifact is part of, and {@code <library>} is the
 * name of the library.
 */
class VersionLoader extends CacheLoader<String, String> {
    public static final String NOT_FOUND = "VERSION-NOT-FOUND";

    private final Iterable<String> packages;

    VersionLoader(Iterable<String> packages) {
        this.packages = packages;
    }

    @Override
    public String load(String library) throws Exception {
        for (String pkg : packages) {
            String found = tryToLoadFrom("META-INF/maven/%s/%s/pom.properties", pkg, library);
            if (found != null) {
                return found;
            }
        }

        return NOT_FOUND;
    }

    private String tryToLoadFrom(String format, String searchPackage, String library) {
        String path = String.format(format, searchPackage, library);
        URL url;
        try {
            url = Resources.getResource(path);
        } catch (IllegalArgumentException e) {
            return null;
        }

        try {
            Closer closer = Closer.create();
            InputStream in = closer.register(url.openStream());
            try {
                Properties props = new Properties();
                props.load(in);

                return props.getProperty("version");
            } finally {
                closer.close();
            }
        } catch (IOException e) {
            return null;
        }
    }
}