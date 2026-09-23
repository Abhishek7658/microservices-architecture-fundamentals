package product_service.security;

import java.io.IOException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        System.out.println("===== JWT FILTER =====");
        System.out.println("Request: " + request.getMethod() + " " + request.getRequestURI());
        System.out.println("Authorization header present: " + (authHeader != null));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("No valid Bearer header found");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        System.out.println("Bearer token received");
        System.out.println("Token length: " + token.length());

        boolean valid = jwtService.isTokenValid(token);

        System.out.println("JWT valid: " + valid);

        if (valid) {

            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            System.out.println("JWT email: " + email);
            System.out.println("JWT role: " + role);

            var authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            System.out.println(
                    "Authentication set: "
                    + SecurityContextHolder.getContext()
                            .getAuthentication()
            );
        }

        System.out.println(
                "BEFORE FILTER CHAIN: "
                + SecurityContextHolder.getContext().getAuthentication()
        );

        filterChain.doFilter(request, response);

        System.out.println(
                "AFTER FILTER CHAIN - HTTP STATUS: "
                + response.getStatus()
        );

        System.out.println(
                "AFTER FILTER CHAIN AUTHENTICATION: "
                + SecurityContextHolder.getContext().getAuthentication()
        );
    }
}