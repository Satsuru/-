package by.bsuir.platform.identity;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Failures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class IdentityService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IdentityService.class);
    private final IdentityRepository repository;

    public IdentityService(IdentityRepository repository) {
        this.repository = repository;
    }

    public Api.UserView me(long userId) {
        return repository.find(userId);
    }

    public Api.AuthPayload register(String email, String password, String fullName, String phone) {
        if (email == null || !email.contains("@")) {
            throw new Failures.BadRequestException("Укажите почту");
        }
        if (password == null || password.length() < 4) {
            throw new Failures.BadRequestException("Пароль короче 4 символов");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new Failures.BadRequestException("Укажите имя");
        }
        if (repository.emailTaken(email)) {
            throw new Failures.BadRequestException("Такая почта уже зарегистрирована");
        }
        long id = repository.insertUser(email.trim(), password, fullName.trim(), phone);
        return token(id);
    }

    public Api.AuthPayload login(String email, String password) {
        Long id = repository.findByEmail(email, password);
        if (id == null) {
            throw new Failures.UnauthorizedException("Неверная почта или пароль");
        }
        return token(id);
    }

    private Api.AuthPayload token(long userId) {
        String token = "user-" + userId;
        repository.saveToken(token, userId);
        return new Api.AuthPayload(token, repository.find(userId));
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Identity: своя схема identity, пользователей {}", repository.countUsers());
    }
}
