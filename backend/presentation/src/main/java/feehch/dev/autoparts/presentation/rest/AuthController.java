package feehch.dev.autoparts.presentation.rest;

import feehch.dev.autoparts.application.auth.AuthResult;
import feehch.dev.autoparts.application.auth.AuthService;
import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.application.user.UserView;
import feehch.dev.autoparts.presentation.dto.AuthRequests;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация покупателя")
    public AuthResult register(@Valid @RequestBody AuthRequests.Register request) {
        return authService.register(request.toCommand());
    }

    @PostMapping("/login")
    @Operation(summary = "Вход по email и паролю")
    public AuthResult login(@Valid @RequestBody AuthRequests.Login request) {
        return authService.login(request.toCommand());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление пары токенов по refresh-токену")
    public AuthResult refresh(@Valid @RequestBody AuthRequests.Refresh request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Выход: отзыв refresh-токена и текущего access-токена")
    public void logout(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestBody(required = false) AuthRequests.Logout request) {
        authService.logout(user, request == null ? null : request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Текущий пользователь")
    public UserView me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.currentUser(user.id());
    }
}
