package feehch.dev.autoparts.application.auth;

public record RegisterCommand(String email, String password, String firstName, String lastName, String phone) {
}
