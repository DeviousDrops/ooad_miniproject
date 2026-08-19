package com.pharmacy.security;

import com.pharmacy.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/register", "/css/**").permitAll()
                        .requestMatchers("/h2-console/**").hasRole("ADMIN")
                        .requestMatchers("/dashboard/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/dashboard/pharmacist", "/pharmacist/**").hasRole("PHARMACIST")
                        .requestMatchers("/dashboard/customer", "/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/dashboard/supplier", "/supplier/**").hasRole("SUPPLIER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(roleBasedSuccessHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication
            ) throws IOException, ServletException {
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String role = authority.getAuthority();
                    if ("ROLE_ADMIN".equals(role)) {
                        response.sendRedirect("/dashboard/admin");
                        return;
                    }
                    if ("ROLE_PHARMACIST".equals(role)) {
                        response.sendRedirect("/dashboard/pharmacist");
                        return;
                    }
                    if ("ROLE_CUSTOMER".equals(role)) {
                        response.sendRedirect("/dashboard/customer");
                        return;
                    }
                    if ("ROLE_SUPPLIER".equals(role)) {
                        response.sendRedirect("/dashboard/supplier");
                        return;
                    }
                }
                response.sendRedirect("/");
            }
        };
    }

    // Unified auth: every role resolves through the same DB-backed User hierarchy.
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            String normalizedUsername = username == null ? "" : username.trim();
            return userRepository.findByUsername(normalizedUsername)
                    .map(user -> User.withUsername(user.getUsername())
                            .password(user.getPassword())
                            .roles(user.roleName())
                            .build())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedUsername));
        };
    }
}
