package by.bsuir.platform.identity;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Failures;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@DependsOn("identitySchema")
public class IdentityService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IdentityService.class);
    private final IdentityRepository repository;
    private final PasswordEncoder passwords;
    private final JwtTokens tokens;
    private final Sessions sessions;

    public IdentityService(IdentityRepository repository, PasswordEncoder passwords, JwtTokens tokens, Sessions sessions) {
        this.repository = repository;
        this.passwords = passwords;
        this.tokens = tokens;
        this.sessions = sessions;
    }

    @PostConstruct
    void hashSeedPasswords() {
        repository.hashPlainPasswords(passwords::encode);
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
        long id = repository.insertUser(email.trim(), passwords.encode(password), fullName.trim(), phone);
        return issue(id);
    }

    public Api.AuthPayload login(String email, String password) {
        IdentityRepository.Credential row = repository.credential(email);
        if (row == null || password == null || !passwords.matches(password, row.hash())) {
            throw new Failures.UnauthorizedException("Неверная почта или пароль");
        }
        return issue(row.id());
    }

    public Api.UserView verify(String token) {
        var claims = tokens.parse(token).getPayload();
        if (!sessions.active(claims.getId())) {
            throw new Failures.UnauthorizedException("Сессия не активна");
        }
        return repository.find(Long.parseLong(claims.getSubject()));
    }

    public void logout(String token) {
        sessions.delete(tokens.parse(token).getPayload().getId());
    }

    private Api.AuthPayload issue(long userId) {
        String jti = UUID.randomUUID().toString();
        sessions.save(jti, userId);
        return new Api.AuthPayload(tokens.issue(userId, jti), repository.find(userId));
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Identity: своя схема identity, пользователей {}", repository.countUsers());
    }
}
