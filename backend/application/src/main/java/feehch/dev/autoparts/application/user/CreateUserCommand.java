package feehch.dev.autoparts.application.user;

import feehch.dev.autoparts.domain.user.Role;

import java.util.Set;

public record CreateUserCommand(String email, String password, String firstName, String lastName, String phone,
                                Set<Role> roles) {
}
