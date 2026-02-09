package galacticos_app_back.galacticos.config;

import galacticos_app_back.galacticos.security.CustomUserDetailsService;
import galacticos_app_back.galacticos.security.JwtAuthenticationEntryPoint;
import galacticos_app_back.galacticos.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // Rutas públicas que no requieren autenticación
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/api/public/**",
            "/api/test/**",  // ⚠️ Endpoints de prueba - REMOVER EN PRODUCCIÓN
            "/api/wompi/webhook",
            "/api/wompi/config",
            "/api/wompi/payment-result",
            "/api/wompi/integrity-signature",
            "/api/wompi/generate-reference",
            "/api/wompi/transaction/**",
            "/uploads/**",
            "/assets/**",
            "/static/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
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
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Configuración MINIMALISTA para evitar duplicados
        // ⚠️ SOLO UN ORIGEN - el de tu frontend en producción
        config.setAllowedOrigins(Collections.singletonList(
            "http://galacticos-frontend.s3-website-us-east-1.amazonaws.com"
        ));
        
        // Para desarrollo local, puedes usar:
        // config.setAllowedOrigins(Arrays.asList(
        //     "http://galacticos-frontend.s3-website-us-east-1.amazonaws.com",
        //     "http://localhost:4200",
        //     "http://localhost:3000"
        // ));
        
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        config.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "Accept"
        ));
        
        config.setExposedHeaders(Arrays.asList(
            "Authorization"
        ));
        
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 1 hora
        
        // IMPORTANTE: Solo registrar en /** para evitar conflictos
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ⚠️ IMPORTANTE: NO usar .cors() aquí, ya que inyectamos el CorsFilter manualmente
            .csrf(csrf -> csrf.disable())
            
            // Agregar el CorsFilter MANUALMENTE como primer filtro
            .addFilterBefore(corsFilter(), UsernamePasswordAuthenticationFilter.class)
            
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(unauthorizedHandler)
            )
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .anyRequest().authenticated()
            )
            .authenticationProvider(daoAuthenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}