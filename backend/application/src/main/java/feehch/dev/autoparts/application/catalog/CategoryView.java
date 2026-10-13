package feehch.dev.autoparts.application.catalog;

import feehch.dev.autoparts.domain.catalog.Category;

public record CategoryView(Long id, String name, String slug, String description, Long parentId) {

    public static CategoryView from(Category c) {
        return new CategoryView(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getParentId());
    }
}
