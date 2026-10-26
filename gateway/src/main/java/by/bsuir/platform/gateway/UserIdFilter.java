package by.bsuir.platform.gateway;

import by.bsuir.platform.contracts.Clients;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserIdFilter extends OncePerRequestFilter {

    private final Clients.IdentityClient identity;

    public UserIdFilter(Clients.IdentityClient identity) {
        this.identity = identity;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                Long id = identity.userIdByToken(auth.substring(7).trim());
                if (id != null) {
                    CurrentUser.set(id);
                }
            }
            if (CurrentUser.requireOrNull() == null) {
                String raw = request.getHeader("X-User-Id");
                if (raw != null && !raw.isBlank()) {
                    CurrentUser.set(Long.valueOf(raw.trim()));
                }
            }
            chain.doFilter(request, response);
        } finally {
            CurrentUser.clear();
        }
    }
}
