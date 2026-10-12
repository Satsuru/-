package by.bsuir.platform.common;

public final class CurrentUser {

    private static final ThreadLocal<Long> ID = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(Long id) {
        ID.set(id);
    }

    public static long require() {
        Long id = ID.get();
        if (id == null) {
            throw new UnauthorizedException("Передайте заголовок X-User-Id");
        }
        return id;
    }

    public static void clear() {
        ID.remove();
    }
}
