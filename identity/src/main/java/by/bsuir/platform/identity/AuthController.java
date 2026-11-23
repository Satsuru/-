package by.bsuir.platform.identity;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Failures;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// REST API сервиса авторизации. Основная логика находится в IdentityService.
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final IdentityService identity;

    public AuthController(IdentityService identity) {
        this.identity = identity;
    }

    // Регистрация нового пользователя
    @PostMapping("/register")
    public Api.AuthPayload register(@RequestBody RegisterRequest body) {
        return identity.register(body.email(), body.password(), body.fullName(), body.phone());
    }

    // Проверка логина и пароля, выдача JWT
    @PostMapping("/login")
    public Api.AuthPayload login(@RequestBody LoginRequest body) {
        return identity.login(body.email(), body.password());
    }

    // Проверка токена и активной сессии
    @PostMapping("/verify")
    public Api.UserView verify(@RequestBody TokenRequest body) {
        return identity.verify(body.token());
    }

    // Удаление сессии из Redis
    @PostMapping("/logout")
    public void logout(@RequestBody TokenRequest body) {
        identity.logout(body.token());
    }

    // Получение пользователя и его ролей
    @GetMapping("/users/{id}")
    public Api.UserView user(@PathVariable long id) {
        return identity.me(id);
    }

    // Тела запросов
    public record RegisterRequest(String email, String password, String fullName, String phone) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record TokenRequest(String token) {
    }

    // Обработка ошибок сервиса
    @RestControllerAdvice
    static class Errors {

        @ExceptionHandler(Failures.BadRequestException.class)
        ResponseEntity<Map<String, String>> bad(Failures.BadRequestException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }

        @ExceptionHandler(Failures.UnauthorizedException.class)
        ResponseEntity<Map<String, String>> denied(Failures.UnauthorizedException ex) {
            return ResponseEntity.status(401).body(Map.of("message", ex.getMessage()));
        }

        @ExceptionHandler(Failures.NotFoundException.class)
        ResponseEntity<Map<String, String>> missing(Failures.NotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of("message", ex.getMessage()));
        }
    }
}
