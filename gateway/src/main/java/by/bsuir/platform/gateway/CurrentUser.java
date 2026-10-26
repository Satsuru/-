package by.bsuir.platform.gateway;

import by.bsuir.platform.contracts.Failures;

public final class CurrentUser {

    private static final ThreadLocal<Long> ID = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(Long id) {
        ID.set(id);
    }

    public static Long requireOrNull() {
        return ID.get();
    }

    public static long require() {
        Long id = ID.get();
        if (id == null) {
            throw new Failures.UnauthorizedException("Передайте заголовок X-User-Id");
        }
        return id;
    }

    public static void clear() {
        ID.remove();
    }
}
