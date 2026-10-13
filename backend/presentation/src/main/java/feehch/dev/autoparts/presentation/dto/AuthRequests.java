package feehch.dev.autoparts.presentation.dto;

import feehch.dev.autoparts.application.auth.LoginCommand;
import feehch.dev.autoparts.application.auth.RegisterCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Запросы аутентификации.
 */
public final class AuthRequests {

    public static final String PHONE_REGEX =
            "^$|^(\\+?375|80)[\\s\\-()]*(17|25|29|33|44|15|16|21|22|23)[\\s\\-()]*(\\d[\\s\\-]*){7}$";
    public static final String PHONE_MESSAGE = "укажите белорусский номер: +375 (XX) XXX-XX-XX";

    private AuthRequests() {
    }

    public record Register(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Pattern(regexp = PHONE_REGEX, message = AuthRequests.PHONE_MESSAGE) String phone) {

        public RegisterCommand toCommand() {
            return new RegisterCommand(email, password, firstName, lastName, phone);
        }
    }

    public record Login(@NotBlank @Email String email, @NotBlank String password) {

        public LoginCommand toCommand() {
            return new LoginCommand(email, password);
        }
    }

    public record Refresh(@NotBlank String refreshToken) {
    }

    public record Logout(String refreshToken) {
    }
}
