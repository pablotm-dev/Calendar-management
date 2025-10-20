package com.calendar_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Profile padrão (qualquer coisa que não seja "calendar")
     * -> Tudo público.
     */
    @Bean
    @Profile("!calendar")
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/", "/health", "/error",
                                "/oauth2/**", "/login/**" // mantém fluxo do Google acessível
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                // opcional: deixa o login OAuth2 disponível, mas nada exige login
                .oauth2Login(oauth -> {});

        return http.build();
    }

    /**
     * Profile "calendar"
     * -> Continua tudo público, mas mantém o fluxo OAuth2 habilitado.
     */
    @Bean
    @Profile("calendar")
    public SecurityFilterChain calendarSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/", "/health", "/error",
                                "/oauth2/**", "/login/**", "/api/auth/status"
                        ).permitAll()
                        .anyRequest().permitAll() // <<< trocado de authenticated() para permitAll()
                )
                .oauth2Login(oauth -> oauth.defaultSuccessUrl("/", true))
                .logout(logout -> logout.logoutSuccessUrl("/"));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Se você precisa de wildcard para Vercel, use AllowedOriginPatterns:
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "https://calendar-management-front-nn.vercel.app",
                "http://calendar-management-front-nn.vercel.app",
                "https://*.vercel.app",
                "http://*.vercel.app"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
