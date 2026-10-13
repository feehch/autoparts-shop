package feehch.dev.autoparts.application.user;

public record ChangePasswordCommand(String currentPassword, String newPassword) {
}
