package ch.zhaw.trueyield.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String jwtIssuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwtJwkSetUri;

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Value("${auth0.audience}")
    private String auth0Audience;

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF disabled: this is a stateless REST API authenticated via Bearer JWT tokens.
        // Browsers never send cookies with these requests, so CSRF attacks are not applicable.
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        if (!StringUtils.hasText(jwtIssuerUri) && !StringUtils.hasText(jwtJwkSetUri)) {
            throw new IllegalStateException("Missing JWT configuration: set issuer-uri or jwk-set-uri");
        }

        http.authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/api/chat").authenticated()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        JwtDecoder candidate;
        if (StringUtils.hasText(jwtJwkSetUri)) {
            candidate = NimbusJwtDecoder.withJwkSetUri(jwtJwkSetUri).build();
        } else if (StringUtils.hasText(jwtIssuerUri)) {
            candidate = JwtDecoders.fromIssuerLocation(jwtIssuerUri);
        } else {
            throw new IllegalStateException("Missing JWT configuration: set issuer-uri or jwk-set-uri");
        }

        if (!(candidate instanceof NimbusJwtDecoder decoder)) {
            throw new IllegalStateException("Unsupported JWT decoder implementation");
        }

        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>("aud",
                        audiences -> audiences != null && audiences.contains(auth0Audience));
        OAuth2TokenValidator<Jwt> validator = StringUtils.hasText(jwtIssuerUri)
                ? new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(jwtIssuerUri), audienceValidator)
                : new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefault(), audienceValidator);
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(
                List.of(allowedOrigins.split(",")).stream()
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        // Normalize role values: "compliance officer" -> ROLE_compliance-officer
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = extractRoles(jwt);
            if (roles == null) return List.of();
            return roles.stream()
                    .map(r -> r.trim().toLowerCase().replace(" ", "-"))
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());
        });
        return jwtConverter;
    }

    private List<String> extractRoles(Jwt jwt) {
        List<String> directRoles = jwt.getClaimAsStringList("user_roles");
        if (directRoles != null) {
            return directRoles;
        }

        Object namespacedRoles = jwt.getClaims().entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("/user_roles"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(jwt.getClaims().get("roles"));
        if (namespacedRoles instanceof List<?> values) {
            return values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }
}
