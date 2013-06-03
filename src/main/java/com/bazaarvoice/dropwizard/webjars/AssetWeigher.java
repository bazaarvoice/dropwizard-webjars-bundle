package com.bazaarvoice.dropwizard.webjars;

import com.google.common.cache.Weigher;

/** Weigh an asset according to the number of bytes it contains. */
class AssetWeigher implements Weigher<AssetId, Asset> {
    @Override
    public int weigh(AssetId key, Asset asset) {
        // return file size in bytes
        return (asset.bytes != null) ? asset.bytes.length : 0;
    }
}