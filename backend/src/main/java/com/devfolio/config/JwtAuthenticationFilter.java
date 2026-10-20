package com.devfolio.config;

import com.devfolio.model.User;
import com.devfolio.repository.UserRepository;
import com.devfolio.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");
        System.out.println("JWT FILTER EXECUTED");
        System.out.println("Authorization header present: "
                + (authorizationHeader != null));

        if (authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")) {

            String token = authorizationHeader.substring(7);

            try {
                if (jwtService.validateToken(token) != null) {

                    String email = jwtService.extractEmail(token);
                    System.out.println("JWT email extracted: " + (email != null));

                    System.out.println("USER FOUND IN DATABASE: "
                            + userRepository.findByEmail(email).isPresent());

                    userRepository.findByEmail(email).ifPresent(user -> {

                        System.out.println("USER ROLE: " + user.getRole());

                        String role = user.getRole();

                        if (role != null
                                && (role.equals("USER")
                                || role.equals("ADMIN"))) {

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            user.getEmail(),
                                            null,
                                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                    );

                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            System.out.println("JWT AUTHENTICATION SUCCESS");


                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("JWT validation failed: "
                        + e.getClass().getSimpleName());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}