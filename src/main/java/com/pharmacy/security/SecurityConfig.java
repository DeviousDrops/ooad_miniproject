package com.pharmacy.security;

import com.pharmacy.repository.CustomerRepository;
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
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/dashboard/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/dashboard/pharmacist", "/pharmacist/**").hasRole("PHARMACIST")
                        .requestMatchers("/dashboard/customer", "/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/dashboard/supplier", "/supplier/**").hasRole("SUPPLIER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/pharmacist/**").hasAnyRole("ADMIN", "PHARMACIST")
                        .requestMatchers("/api/customer/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers("/api/supplier/**").hasAnyRole("ADMIN", "SUPPLIER")
                        .requestMatchers("/api/billing/**", "/api/inventory/**").authenticated()
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

    @Bean
        public UserDetailsService userDetailsService(CustomerRepository customerRepository) {
        return username -> {
            String normalizedUsername = username == null ? "" : username.trim();

            if ("admin".equals(normalizedUsername)) {
            return User.withUsername("admin")
                .password("{noop}admin123")
                .roles("ADMIN")
                .build();
            }

            if ("pharmacist".equals(normalizedUsername)) {
            return User.withUsername("pharmacist")
                .password("{noop}pharma123")
                .roles("PHARMACIST")
                .build();
            }

            if ("supplier".equals(normalizedUsername)) {
            return User.withUsername("supplier")
                .password("{noop}supplier123")
                .roles("SUPPLIER")
                .build();
            }

            return customerRepository.findByPhone(normalizedUsername)
                    .map(customer -> User.withUsername(customer.getPhone())
                            .password(customer.getPassword() != null && customer.getPassword().startsWith("{")
                                    ? customer.getPassword()
                                    : "{noop}" + customer.getPassword())
                            .roles("CUSTOMER")
                            .build())
                    .or(() -> customerRepository.findFirstByNameIgnoreCase(normalizedUsername)
                        .map(customer -> User.withUsername(customer.getPhone())
                            .password(customer.getPassword() != null && customer.getPassword().startsWith("{")
                                ? customer.getPassword()
                                : "{noop}" + customer.getPassword())
                            .roles("CUSTOMER")
                            .build()))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedUsername));
        };
    }
}
