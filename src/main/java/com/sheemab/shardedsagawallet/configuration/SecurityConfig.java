package com.sheemab.shardedsagawallet.configuration;


import com.sheemab.shardedsagawallet.services.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF disable — REST API hai, browser forms nahi
                .csrf(AbstractHttpConfigurer::disable)

                // URL rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — token ki zaroorat nahi
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/swagger-ui/**",    // Swagger docs
                                "/v3/api-docs/**"    // OpenAPI spec
                        ).permitAll()
                        // Baaki sab protected
                        .anyRequest().authenticated()
                )

                // Session STATELESS — server koi session nahi rakhega
                // Har request apna JWT lekar aayegi
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Apna AuthenticationProvider set karo
                .authenticationProvider(authenticationProvider())

                // JwtFilter ko UsernamePasswordAuthenticationFilter se PEHLE lagao
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // DB se user load karne ka tarika
        provider.setUserDetailsService(userDetailsService);
        // Password verify karne ka tarika
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt — industry standard password hashing
        return new BCryptPasswordEncoder();
    }
}
