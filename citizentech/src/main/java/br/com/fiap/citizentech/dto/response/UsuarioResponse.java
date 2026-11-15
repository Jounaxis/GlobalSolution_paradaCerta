package br.com.fiap.citizentech.dto.response;

import java.time.LocalDateTime;

import br.com.fiap.citizentech.model.Usuario;
import br.com.fiap.citizentech.model.enums.Perfil;

/** Dados publicos do usuario. O hash da senha nunca sai da API. */
public record UsuarioResponse(
		Long id,
		String nome,
		String email,
		Perfil perfil,
		String expoPushToken,
		LocalDateTime criadoEm) {

	public static UsuarioResponse from(Usuario usuario) {
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getPerfil(),
				usuario.getExpoPushToken(),
				usuario.getCriadoEm());
	}

}
