package by.bsuir.platform.identity;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class Sessions {

    private static final Duration TTL = Duration.ofHours(2);
    private final StringRedisTemplate redis;

    public Sessions(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(String jti, long userId) {
        redis.opsForValue().set(key(jti), Long.toString(userId), TTL);
    }

    public boolean active(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(key(jti)));
    }

    public void delete(String jti) {
        redis.delete(key(jti));
    }

    private String key(String jti) {
        return "session:" + jti;
    }
}
