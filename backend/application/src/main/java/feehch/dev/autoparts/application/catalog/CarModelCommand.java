package feehch.dev.autoparts.application.catalog;

public record CarModelCommand(Long makeId, String name, Integer yearFrom, Integer yearTo) {
}
