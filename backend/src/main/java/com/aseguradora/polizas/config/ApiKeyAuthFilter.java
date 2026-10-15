package com.aseguradora.polizas.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica la petición si trae el header {@code x-api-key} correcto. La comparación es en tiempo constante para no
 * filtrar información por tiempos de respuesta. Si la key falta o no coincide, la petición sigue sin autenticación y
 * Spring Security responde 401.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

	public static final String HEADER = "x-api-key";

	private final byte[] apiKeyEsperada;

	public ApiKeyAuthFilter(String apiKey) {
		this.apiKeyEsperada = apiKey.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		var recibida = request.getHeader(HEADER);
		if (recibida != null
				&& MessageDigest.isEqual(apiKeyEsperada, recibida.getBytes(StandardCharsets.UTF_8))) {
			var auth = UsernamePasswordAuthenticationToken.authenticated("api-client", null,
					List.of(new SimpleGrantedAuthority("ROLE_API")));
			var context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(auth);
			SecurityContextHolder.setContext(context);
		}
		chain.doFilter(request, response);
	}
}
