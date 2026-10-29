package pe.edu.upeu.autoandes.config;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import pe.edu.upeu.autoandes.security.*;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt, UserDetailsService usuarios,
            RestAuthenticationEntryPoint entry, RestAccessDeniedHandler denied,
            CorsConfigurationSource cors) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(c -> c.configurationSource(cors))
                .exceptionHandling(e -> e.authenticationEntryPoint(entry).accessDeniedHandler(denied))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/clientes").hasAuthority("VEHICULO_LEER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/servicios", "/api/v1/servicios/*").hasAuthority("SERVICIO_LEER")
                        .requestMatchers("/api/v1/servicios", "/api/v1/servicios/**").hasAuthority("SERVICIO_ESCRIBIR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehiculos", "/api/v1/vehiculos/*").hasAuthority("VEHICULO_LEER")
                        .requestMatchers("/api/v1/vehiculos", "/api/v1/vehiculos/**").hasAuthority("VEHICULO_ESCRIBIR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ordenes").hasAuthority("ORDEN_REGISTRAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/ordenes/*/anular").hasAuthority("ORDEN_ANULAR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ordenes", "/api/v1/ordenes/*").hasAuthority("ORDEN_LEER")
                        .requestMatchers("/api/v1/reportes/**").hasAuthority("REPORTE_VER")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwt, usuarios, entry), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

