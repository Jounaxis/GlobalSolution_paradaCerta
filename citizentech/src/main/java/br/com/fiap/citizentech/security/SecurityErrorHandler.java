package br.com.fiap.citizentech.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Os erros de seguranca acontecem nos filtros, antes de chegar no controller.
 * Esta classe repassa esses erros para o GlobalExceptionHandler, para que 401 e 403
 * saiam no mesmo formato JSON (ErroResponse) dos outros erros da API.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final HandlerExceptionResolver resolver;

	public SecurityErrorHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	/** 401: token ausente, invalido ou expirado. */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException ex) throws IOException {
		resolver.resolveException(request, response, null, ex);
	}

	/** 403: token valido, mas o papel nao permite acessar o recurso. */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException ex) throws IOException {
		resolver.resolveException(request, response, null, ex);
	}

}
