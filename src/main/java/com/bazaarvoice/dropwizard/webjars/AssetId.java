package com.bazaarvoice.dropwizard.webjars;

import com.google.common.base.Objects;

class AssetId {
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

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("library", library)
                .add("resource", resource)
                .toString();
    }
}