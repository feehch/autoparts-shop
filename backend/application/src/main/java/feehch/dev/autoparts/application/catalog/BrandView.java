package feehch.dev.autoparts.application.catalog;

import feehch.dev.autoparts.domain.catalog.Brand;

public record BrandView(Long id, String name, String country, String description) {

    public static BrandView from(Brand b) {
        return new BrandView(b.getId(), b.getName(), b.getCountry(), b.getDescription());
    }
}
