package feehch.dev.autoparts.application.catalog;

public record CategoryCommand(String name, String slug, String description, Long parentId) {
}
