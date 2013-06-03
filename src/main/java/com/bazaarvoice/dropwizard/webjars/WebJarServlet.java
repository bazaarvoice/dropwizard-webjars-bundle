package com.bazaarvoice.dropwizard.webjars;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Buffer;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.EntityTag;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebJarServlet extends HttpServlet {
    /** The URL prefix that webjars are served out of. */
    public static final String URL_PREFIX = "/webjars/";

    /** The default package that webjars.org built webjars are found in. */
    @VisibleForTesting
    static String[] DEFAULT_WEBJAR_PACKAGES = {"org.webjars"};

    /** A path parser that can determine what library and library resource a particular path is for. */
    private static final Pattern PATH_PARSER = Pattern.compile(URL_PREFIX + "([^/]+)/(.+)");

    /** An If-None-Match header parser, splits the header into the multiple eTags that might be present. */
    private static final Splitter IF_NONE_MATCH_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

    private final transient LoadingCache<AssetId, Asset> cache;

    public WebJarServlet() {
        this(null, null);
    }

    @SuppressWarnings("unchecked")
    public WebJarServlet(CacheBuilder builder, Iterable<String> packages) {
        if (builder == null) {
            builder = CacheBuilder.newBuilder()
                    .maximumWeight(2 * 1024 * 1024)
                    .expireAfterAccess(5, TimeUnit.MINUTES);
        }

        if (packages == null || Iterables.isEmpty(packages)) {
            packages = ImmutableList.copyOf(DEFAULT_WEBJAR_PACKAGES);
        }

        AssetLoader loader = new AssetLoader(new VersionLoader(packages));
        cache = builder.weigher(new AssetSizeWeigher()).build(loader);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            handle(req, resp);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = getFullPath(req);
        Matcher m = PATH_PARSER.matcher(path);
        if (!m.matches()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        AssetId id = new AssetId(m.group(1), m.group(2));
        Asset asset = cache.getUnchecked(id);

        // We know we've found the asset.  No matter what happens, make sure we send back its last modification time
        // as well as its eTag
        resp.setDateHeader(HttpHeaders.LAST_MODIFIED, asset.lastModifiedTime);
        resp.setHeader(HttpHeaders.ETAG, new EntityTag(asset.eTag).toString());

        // Check the ETags to see if the resource has changed...
        String ifNoneMatch = req.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (ifNoneMatch != null) {
            for (String eTag : IF_NONE_MATCH_SPLITTER.split(ifNoneMatch)) {
                if ("*".equals(eTag) || asset.eTag.equals(getETagValue(eTag))) {
                    // This is the same version the client has
                    resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
            }
        }

        // Check the last modification time...
        if (asset.lastModifiedTime <= req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)) {
            // Theirs is the same, or later than ours
            resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // Send back the correct content type and character encoding headers
        resp.setContentType(asset.mediaType.type() + "/" + asset.mediaType.subtype());
        if (asset.mediaType.charset().isPresent()) {
            resp.setCharacterEncoding(asset.mediaType.charset().get().toString());
        }

        ServletOutputStream output = resp.getOutputStream();
        try {
            output.write(asset.bytes);
        } finally {
            output.close();
        }
    }

    private static String getFullPath(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder(request.getServletPath());
        if (request.getPathInfo() != null) {
            sb.append(request.getPathInfo());
        }

        return sb.toString();
    }

    private static String getETagValue(String eTag) {
        try {
            return EntityTag.valueOf(eTag).getValue();
        } catch (Exception e) {
            return null;
        }
    }

    /** Weigh an asset according to the number of bytes it contains. */
    private static final class AssetSizeWeigher implements Weigher<AssetId, Asset> {
        @Override
        public int weigh(AssetId key, Asset asset) {
            // return file sze in bytes
            return asset.bytes.length;
        }
    }

    private static final class AssetId {
        public final String library;
        public final String resource;

        public AssetId(String library, String resource) {
            this.library = library;
            this.resource = resource;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof AssetId)) return false;

            AssetId id = (AssetId) o;
            return Objects.equal(library, id.library) && Objects.equal(resource, id.resource);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(library, resource);
        }
    }

    private static final class Asset {
        public final byte[] bytes;
        public final MediaType mediaType;
        public final String eTag;
        public final long lastModifiedTime;

        public Asset(byte[] bytes, MediaType mediaType) {
            this.bytes = bytes;
            this.mediaType = mediaType;
            this.eTag = Hashing.murmur3_128().hashBytes(bytes).toString();
            this.lastModifiedTime = (new Date().getTime() / 1000) * 1000;  // Ignore milliseconds
        }
    }

    private static class AssetLoader extends CacheLoader<AssetId, Asset> {
        // For determining content type and content encoding
        private static final MimeTypes MIME_TYPES = new MimeTypes();
        private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.HTML_UTF_8;
        private static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

        private final CacheLoader<String, String> versionLoader;
        private final LoadingCache<String, String> versionCache;

        private AssetLoader(CacheLoader<String, String> versionLoader) {
            this.versionLoader = versionLoader;
            this.versionCache = CacheBuilder.newBuilder()
                    .maximumSize(10)
                    .build(this.versionLoader);
        }

        @Override
        public Asset load(AssetId id) throws Exception {
            String version;
            try {
                version = versionCache.get(id.library);
            } catch (ExecutionException e) {
                return null;
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
                    return null;
                }

                version = version.substring(0, hyphen);
            }
            while (true);
        }

        private MediaType getMediaType(String path) {
            Buffer mimeType = MIME_TYPES.getMimeByExtension(path);
            if (mimeType == null) {
                return DEFAULT_MEDIA_TYPE;
            }

            MediaType mediaType;
            try {
                mediaType = MediaType.parse(mimeType.toString());

                if (mediaType.is(MediaType.ANY_TEXT_TYPE)) {
                    mediaType = mediaType.withCharset(DEFAULT_CHARSET);
                }
            } catch (IllegalArgumentException e) {
                return DEFAULT_MEDIA_TYPE;
            }

            return mediaType;
        }
    }

    /**
     * Determines the version of the webjar that's in the classpath for a particular library.
     * <p/>
     * The version of a webjar can be determined by looking at the <code>pom.properties</code> file located at
     * {@code META-INF/maven/<group>/<library>/pom.properties}.
     * <p/>
     * Where {@code <group>} is the name of the maven group the webjar artifact is part of, and {@code <library>} is the
     * name of the library.
     */
    private static class VersionLoader extends CacheLoader<String, String> {
        private final Iterable<String> packages;

        private VersionLoader(Iterable<String> packages) {
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
            return null;
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
}
