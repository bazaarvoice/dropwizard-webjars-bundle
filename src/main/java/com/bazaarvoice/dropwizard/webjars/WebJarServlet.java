package com.bazaarvoice.dropwizard.webjars;

import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.EntityTag;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A servlet that will load resources from WebJars found in the classpath.  In order to make it more convenient to
 * use a WebJar, this servlet will automatically determine the version of the WebJar present in the classpath and make
 * it so that you don't need to explicitly specify a version number as part of the URL.  This allows WebJars to be
 * upgraded entirely via maven dependencies without having to update all of the references to the WebJar in your UI
 * code.
 */
public class WebJarServlet extends HttpServlet {
    /** The URL prefix that webjars are served out of. */
    public static final String DEFAULT_URL_PREFIX = "/webjars/";

    /** The default maven group(s) that WebJars are searched for in. */
    public static final String[] DEFAULT_MAVEN_GROUPS = { "org.webjars" };

    /** A path parser that can determine what library and library resource a particular path is for. */
    private final Pattern pathParser;

    /** An If-None-Match header parser, splits the header into the multiple ETags that might be present. */
    private static final Splitter IF_NONE_MATCH_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

    private static final Logger LOG = LoggerFactory.getLogger(WebJarServlet.class);

    private final transient LoadingCache<AssetId, Asset> cache;

    private final String urlPrefix;

    @SuppressWarnings("unchecked")
    public WebJarServlet(CacheBuilder builder, Iterable<String> groups, final String urlPrefix) {
        if (builder == null) {
            builder = CacheBuilder.newBuilder()
                    .maximumWeight(5 * 1024 * 1024)
                    .expireAfterAccess(5, TimeUnit.MINUTES);
        }

        if (groups == null || Iterables.isEmpty(groups)) {
            groups = ImmutableList.copyOf(DEFAULT_MAVEN_GROUPS);
        }

        AssetLoader loader = new AssetLoader(new VersionLoader(groups));
        cache = builder.weigher(new AssetWeigher()).build(loader);
        this.urlPrefix = (urlPrefix == null ? DEFAULT_URL_PREFIX : urlPrefix);
        this.pathParser = Pattern.compile(this.urlPrefix+ "([^/]+)/(.+)");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            handle(req, resp);
        } catch (Exception e) {
            LOG.info("Error processing request: {}", req, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = getFullPath(req);

        // Check to see if this is a valid path that we know how to deal with, if so parse out the library and resource
        Matcher m = pathParser.matcher(path);
        if (!m.matches()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // The path is valid, try to load the asset
        AssetId id = new AssetId(m.group(1), m.group(2));
        Asset asset = cache.getUnchecked(id);
        if (asset == AssetLoader.NOT_FOUND) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // We know we've found the asset.  No matter what happens, make sure we send back its last modification time
        // as well as its ETag
        resp.setDateHeader(HttpHeaders.LAST_MODIFIED, asset.lastModifiedTime);
        resp.setHeader(HttpHeaders.ETAG, hash2etag(asset.hash));

        // Check the If-None-Match header to see if any ETags match this resource
        String ifNoneMatch = req.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (ifNoneMatch != null) {
            for (String eTag : IF_NONE_MATCH_SPLITTER.split(ifNoneMatch)) {
                if ("*".equals(eTag) || asset.hash.equals(etag2hash(eTag))) {
                    resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
            }
        }

        // Check the If-Modified-Since header to see if this resource is newer
        if (asset.lastModifiedTime <= req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)) {
            resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // Send back the correct content type and character encoding headers
        resp.setContentType(asset.mediaType.toString());
        if (asset.mediaType.charset().isPresent()) {
            resp.setCharacterEncoding(asset.mediaType.charset().get().toString());
        }

        // Finally write the bytes of the asset out
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

    private static String hash2etag(String hash) {
        return new EntityTag(hash).toString();
    }

    private static String etag2hash(String etag) {
        String hash;

        try {
            hash = EntityTag.valueOf(etag).getValue();
        } catch (Exception e) {
            return null;
        }

        // Jetty insists on adding a -gzip suffix to ETags sometimes.  If it's there, then strip it off.
        if (hash.endsWith("-gzip")) {
            hash = hash.substring(0, hash.length() - 5);
        }

        return hash;
    }
}
