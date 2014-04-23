package com.bazaarvoice.dropwizard.webjars;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.common.net.MediaType;
import org.eclipse.jetty.http.MimeTypes;

import java.net.URL;
import java.nio.charset.Charset;

/** Locates an loads a particular WebJar asset from the classpath. */
class AssetLoader extends CacheLoader<AssetId, Asset> {
    public static final Asset NOT_FOUND = new Asset(null, null);

    // For determining content type and content encoding
    private static final MimeTypes MIME_TYPES = new MimeTypes();
    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.HTML_UTF_8;
    private static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

    private final LoadingCache<String, String> versionCache;

    AssetLoader(CacheLoader<String, String> versionLoader) {
        versionCache = CacheBuilder.newBuilder()
                .maximumSize(10)
                .build(versionLoader);
    }

    @Override
    public Asset load(AssetId id) throws Exception {
        String version = versionCache.getUnchecked(id.library);
        if (VersionLoader.NOT_FOUND.equals(version)) {
            return NOT_FOUND;
        }

        // Sometimes the WebJar has multiple releases which gets represented by a -# suffix to the version number
        // inside of pom.properties.  When this happens, the files inside of the jar don't include the WebJar
        // release number as part of the file path.  For example, the angularjs 1.1.1 WebJar has a version inside of
        // pom.properties of 1.1.1-1.  But the path to angular.js inside of the jar is
        // META-INF/resources/webjars/angularjs/1.1.1/angular.js.
        //
        // Alternatively sometimes the developer of the library includes a -suffix in the true library version.  In
        // these cases the WebJar pom.properties will include that suffix in the version number, and the file paths
        // inside of the jar will also include the suffix.  For example, the backbone-marionette 1.0.0-beta6 WebJar
        // has a version inside of pom.properties of 1.0.0-beta6.  The path to backbone.marionette.js is also
        // META-INF/resources/webjars/backbone-marionette/1.0.0-beta6/backbone.marionette.js.
        //
        // So based on the data inside of pom.properties it's going to be impossible to determine whether a -suffix
        // should be stripped off or not.  A reasonable heuristic however is going to be to just keep trying over
        // and over starting with the most specific version number, then stripping a suffix off at a time until
        // there are no more suffixes and the right version number is determined.
        do {
            String path = String.format("META-INF/resources/webjars/%s/%s/%s", id.library, version, id.resource);

            try {
                URL resource = Resources.getResource(path);

                // Determine the media type of this resource
                MediaType mediaType = getMediaType(path);

                // We know that this version was valid.  Update the version cache to make sure that we remember it
                // for next time around.
                versionCache.put(id.library, version);

                return new Asset(ByteStreams.toByteArray(resource.openStream()), mediaType);
            } catch (IllegalArgumentException e) {
                // ignored
            }

            // Trim a suffix off of the version number
            int hyphen = version.lastIndexOf('-');
            if (hyphen == -1) {
                return NOT_FOUND;
            }

            version = version.substring(0, hyphen);
        }
        while (true);
    }

    private MediaType getMediaType(String path) {
        String mimeType = MIME_TYPES.getMimeByExtension(path);
        if (mimeType == null) {
            return DEFAULT_MEDIA_TYPE;
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parse(mimeType);

            if (mediaType.is(MediaType.ANY_TEXT_TYPE)) {
                mediaType = mediaType.withCharset(DEFAULT_CHARSET);
            }
        } catch (IllegalArgumentException e) {
            return DEFAULT_MEDIA_TYPE;
        }

        return mediaType;
    }
}