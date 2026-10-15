package com.aseguradora.polizas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad sin estado basada en API key. Público: solo {@code /actuator/health}. Todo lo demás exige
 * {@code x-api-key}; sin ella, 401 en formato ProblemDetail (RFC 9457).
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ApiSecurityProperties props) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			.httpBasic(basic -> basic.disable())
			.formLogin(form -> form.disable())
			.logout(logout -> logout.disable())
			.requestCache(cache -> cache.disable())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.addFilterBefore(new ApiKeyAuthFilter(props.apiKey()), AnonymousAuthenticationFilter.class)
			.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**", "/error")
				.permitAll()
				.anyRequest()
				.authenticated())
			.exceptionHandling(e -> e.authenticationEntryPoint(entryPoint()));
		return http.build();
	}

	/** 401 en formato ProblemDetail. getRequestURI() llega codificado, así que no contiene comillas. */
	private AuthenticationEntryPoint entryPoint() {
		return (request, response, ex) -> {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("""
					{"type":"urn:polizas:error:no-autorizado","title":"No autorizado","status":401,\
					"detail":"Falta el header x-api-key o su valor no es válido","instance":"%s"}"""
				.formatted(request.getRequestURI()));
		};
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(ApiSecurityProperties props) {
		var config = new CorsConfiguration();
		config.setAllowedOrigins(props.corsAllowedOrigins());
		config.addAllowedMethod("GET");
		config.addAllowedMethod("POST");
		config.addAllowedHeader("Content-Type");
		config.addAllowedHeader(ApiKeyAuthFilter.HEADER);
		config.addAllowedHeader(CorrelationIdFilter.HEADER);
		config.addExposedHeader(CorrelationIdFilter.HEADER);
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	/** Sin usuarios: evita que Spring Boot genere un usuario con contraseña por defecto. */
	@Bean
	InMemoryUserDetailsManager userDetailsService() {
		return new InMemoryUserDetailsManager();
	}
}
