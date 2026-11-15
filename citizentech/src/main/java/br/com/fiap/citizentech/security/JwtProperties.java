package br.com.fiap.citizentech.security;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do JWT (bloco citizentech.jwt do application.yaml).
 *
 * @param secret            chave HMAC-SHA256; precisa ter pelo menos 32 bytes
 * @param expirationMinutes validade do token em minutos
 * @param issuer            emissor gravado no claim "iss"
 */
@ConfigurationProperties(prefix = "citizentech.jwt")
public record JwtProperties(String secret, long expirationMinutes, String issuer) {

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres");
		}
		if (expirationMinutes <= 0) {
			throw new IllegalStateException("JWT_EXPIRATION_MINUTES precisa ser maior que zero");
		}
	}

}
