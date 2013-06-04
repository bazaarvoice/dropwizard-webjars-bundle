package com.bazaarvoice.dropwizard.webjars;

import com.google.common.hash.Hashing;
import com.google.common.net.MediaType;

import java.util.Date;

class Asset {
    public final byte[] bytes;
    public final MediaType mediaType;
    public final String hash;
    public final long lastModifiedTime;

    public Asset(byte[] bytes, MediaType mediaType) {
        this.bytes = bytes;
        this.mediaType = mediaType;
        this.hash = (bytes != null) ? Hashing.murmur3_128().hashBytes(bytes).toString() : null;
        this.lastModifiedTime = (new Date().getTime() / 1000) * 1000;  // Ignore milliseconds
    }
}