package by.bsuir.platform.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String raw = request.getHeader("X-User-Id");
            if (raw != null && !raw.isBlank()) {
                CurrentUser.set(Long.valueOf(raw.trim()));
            }
            chain.doFilter(request, response);
        } finally {
            CurrentUser.clear();
        }
    }
}
