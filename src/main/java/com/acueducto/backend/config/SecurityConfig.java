package com.acueducto.backend.config;

import com.acueducto.backend.security.JwtAuthenticationEntryPoint;
import com.acueducto.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuracion central de seguridad. Implementa RBAC (3.1): las rutas publicas del
 * portal quedan abiertas sin autenticacion; el resto exige JWT valido, y la autorizacion
 * fina por rol se aplica ademas a nivel de metodo con @PreAuthorize en cada controlador.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    /** Rutas publicas sin restriccion de metodo HTTP (no exponen operaciones de escritura sensibles). */
    private static final String[] PUBLIC_ENDPOINTS_ANY_METHOD = {
            "/api/v1/auth/**",
            "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**",
            "/h2-console/**",
            "/storage/**",
            // Portal publico y consultas sin login (2.7 / 11.3)
            "/api/v1/publico/**",
            "/api/v1/consultas/**",
            "/api/v1/facturas/qr/**",
            "/api/v1/recibos/qr/**",
            "/api/v1/encuestas/*/responder", // responder un formulario es una escritura intencionalmente publica
            "/api/v1/publicaciones/*/reacciones" // reaccionar a una publicacion/imagen es publico, sin login
    };

    /** Rutas de solo lectura que deben quedar publicas unicamente para peticiones GET (2.7 / 11.3 / 13.3). */
    private static final String[] PUBLIC_ENDPOINTS_GET_ONLY = {
            "/api/v1/notificaciones/publicas/**",
            "/api/v1/publicaciones/publicas/**",
            "/api/v1/publicaciones/destacadas/**",
            "/api/v1/publicaciones/categorias/**",
            "/api/v1/publicaciones/etiquetas/**",
            "/api/v1/publicaciones/videos/publicos/**",
            "/api/v1/encuestas/publicas/**",
            "/api/v1/encuestas/codigo/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // necesario para h2-console
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS_ANY_METHOD).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, PUBLIC_ENDPOINTS_GET_ONLY).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (origins.contains("*")) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(origins);
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
