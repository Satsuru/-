package by.bsuir.platform.identity;

import redis.embedded.RedisServer;

public final class EmbeddedRedis {

    private static volatile RedisServer server;

    private EmbeddedRedis() {
    }

    public static void ensure() {
        if (server != null) {
            return;
        }
        try {
            RedisServer candidate = RedisServer.newRedisServer().port(6379).build();
            candidate.start();
            server = candidate;
        } catch (Exception ignored) {
            // Порт уже слушает установленный Redis.
        }
    }
}
