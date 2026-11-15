package br.com.fiap.citizentech.dto.response;

import java.time.Instant;

/**
 * Resposta do login. O app guarda o token e envia em toda chamada no header
 * {@code Authorization: Bearer <token>}.
 */
public record LoginResponse(
		String token,
		String tipo,
		Instant expiraEm,
		UsuarioResponse usuario) {
}
