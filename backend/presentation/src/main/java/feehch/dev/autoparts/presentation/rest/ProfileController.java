package feehch.dev.autoparts.presentation.rest;

import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.application.user.ProfileService;
import feehch.dev.autoparts.application.user.UserView;
import feehch.dev.autoparts.presentation.dto.UserRequests;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Профиль")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserView get(@AuthenticationPrincipal AuthenticatedUser user) {
        return profileService.getProfile(user.id());
    }

    @PutMapping
    public UserView update(@AuthenticationPrincipal AuthenticatedUser user,
                           @Valid @RequestBody UserRequests.UpdateProfile request) {
        return profileService.updateProfile(user.id(), request.toCommand());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody UserRequests.ChangePassword request) {
        profileService.changePassword(user.id(), request.toCommand());
    }
}
