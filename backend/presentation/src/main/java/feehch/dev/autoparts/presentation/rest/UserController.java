package feehch.dev.autoparts.presentation.rest;

import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.application.user.RoleView;
import feehch.dev.autoparts.application.user.UserAdministrationService;
import feehch.dev.autoparts.application.user.UserView;
import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.presentation.dto.UserRequests;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Пользователи и роли (администратор)")
public class UserController {

    private final UserAdministrationService userService;

    public UserController(UserAdministrationService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Page<UserView> search(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) Role role,
                                 @RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer size) {
        return userService.search(q, role, PageQuery.of(page, size));
    }

    @GetMapping("/roles")
    @Operation(summary = "Справочник ролей")
    public List<RoleView> roles() {
        return userService.roles();
    }

    @GetMapping("/{id}")
    public UserView get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создание пользователя с заданными ролями (например, менеджера)")
    public UserView create(@Valid @RequestBody UserRequests.CreateUser request) {
        return userService.create(request.toCommand());
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Назначение ролей пользователю")
    public UserView changeRoles(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                @Valid @RequestBody UserRequests.ChangeRoles request) {
        return userService.changeRoles(actor, id, request.roles());
    }

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Блокировка / разблокировка пользователя")
    public UserView changeEnabled(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                  @Valid @RequestBody UserRequests.ChangeEnabled request) {
        return userService.setEnabled(actor, id, request.enabled());
    }
}
