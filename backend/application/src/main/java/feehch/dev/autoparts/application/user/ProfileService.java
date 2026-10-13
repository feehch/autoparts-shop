package feehch.dev.autoparts.application.user;

import feehch.dev.autoparts.application.port.out.PasswordHasher;
import feehch.dev.autoparts.application.port.out.UnitOfWork;
import feehch.dev.autoparts.application.port.out.UserRepository;
import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.common.NotFoundException;
import feehch.dev.autoparts.domain.user.User;

/**
 * Сценарии работы пользователя со своим профилем.
 */
public class ProfileService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final UnitOfWork unitOfWork;

    public ProfileService(UserRepository users, PasswordHasher passwordHasher, UnitOfWork unitOfWork) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.unitOfWork = unitOfWork;
    }

    public UserView getProfile(Long userId) {
        return UserView.from(load(userId));
    }

    public UserView updateProfile(Long userId, UpdateProfileCommand command) {
        return unitOfWork.inTransaction(() -> {
            User user = load(userId);
            user.updateProfile(command.firstName(), command.lastName(), command.phone());
            return UserView.from(users.save(user));
        });
    }

    public void changePassword(Long userId, ChangePasswordCommand command) {
        unitOfWork.inTransaction(() -> {
            User user = load(userId);
            if (!passwordHasher.matches(command.currentPassword(), user.getPasswordHash())) {
                throw new ConflictException("Текущий пароль указан неверно");
            }
            user.changePasswordHash(passwordHasher.hash(command.newPassword()));
            users.save(user);
        });
    }

    private User load(Long userId) {
        return users.findById(userId).orElseThrow(() -> NotFoundException.of("Пользователь", userId));
    }
}
