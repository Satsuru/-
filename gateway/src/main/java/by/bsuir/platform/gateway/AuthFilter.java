package by.bsuir.platform.gateway;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Clients;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private final Clients.IdentityClient identity;

    public AuthFilter(Clients.IdentityClient identity) {
        this.identity = identity;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7).trim();
                Api.UserView user = identity.userByToken(token);
                if (user != null) {
                    CurrentUser.set(user, token);
                }
            }
            chain.doFilter(request, response);
        } finally {
            CurrentUser.clear();
        }
    }
}
