package by.bsuir.platform.identity;

import by.bsuir.platform.contracts.Failures;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokens {

    private final SecretKey key;

    public JwtTokens(@Value("${auth.jwt.secret:hotel-platform-lab-secret-key-32b}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(long userId, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(Long.toString(userId))
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofHours(2))))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new Failures.UnauthorizedException("Нужна авторизация");
        }
    }
}
