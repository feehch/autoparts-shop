package feehch.dev.autoparts.presentation.dto;

import feehch.dev.autoparts.application.user.ChangePasswordCommand;
import feehch.dev.autoparts.application.user.CreateUserCommand;
import feehch.dev.autoparts.application.user.UpdateProfileCommand;
import feehch.dev.autoparts.domain.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Запросы, связанные с пользователями и профилем.
 */
public final class UserRequests {

    private UserRequests() {
    }

    public record UpdateProfile(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Pattern(regexp = AuthRequests.PHONE_REGEX, message = AuthRequests.PHONE_MESSAGE) String phone) {

        public UpdateProfileCommand toCommand() {
            return new UpdateProfileCommand(firstName, lastName, phone);
        }
    }

    public record ChangePassword(@NotBlank String currentPassword,
                                 @NotBlank @Size(min = 8, max = 100) String newPassword) {

        public ChangePasswordCommand toCommand() {
            return new ChangePasswordCommand(currentPassword, newPassword);
        }
    }

    public record CreateUser(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Pattern(regexp = AuthRequests.PHONE_REGEX, message = AuthRequests.PHONE_MESSAGE) String phone,
            @NotEmpty Set<Role> roles) {

        public CreateUserCommand toCommand() {
            return new CreateUserCommand(email, password, firstName, lastName, phone, roles);
        }
    }

    public record ChangeRoles(@NotEmpty Set<@NotNull Role> roles) {
    }

    public record ChangeEnabled(@NotNull Boolean enabled) {
    }
}
