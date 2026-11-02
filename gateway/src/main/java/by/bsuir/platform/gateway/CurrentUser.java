package by.bsuir.platform.gateway;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Failures;

public final class CurrentUser {

    private static final ThreadLocal<Api.UserView> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(Api.UserView user, String token) {
        USER.set(user);
        TOKEN.set(token);
    }

    public static Api.UserView requireOrNull() {
        return USER.get();
    }

    public static long require() {
        Api.UserView user = USER.get();
        if (user == null || user.id() == null) {
            throw new Failures.UnauthorizedException("Передайте заголовок Authorization: Bearer");
        }
        return user.id();
    }

    public static String token() {
        return TOKEN.get();
    }

    public static boolean hasRole(String role) {
        Api.UserView user = USER.get();
        return user != null && user.roles() != null && user.roles().contains(role);
    }

    public static void clear() {
        USER.remove();
        TOKEN.remove();
    }
}
