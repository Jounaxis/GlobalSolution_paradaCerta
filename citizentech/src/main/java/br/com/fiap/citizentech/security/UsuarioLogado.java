package br.com.fiap.citizentech.security;

import org.springframework.security.oauth2.jwt.Jwt;

import br.com.fiap.citizentech.model.enums.Perfil;

/**
 * Quem esta chamando a API, lido do token JWT validado.
 * Os services usam para aplicar as regras de dono (ex.: cuidador so ve os proprios passageiros).
 */
public record UsuarioLogado(Long id, Perfil perfil) {

	public static UsuarioLogado from(Jwt jwt) {
		return new UsuarioLogado(
				Long.valueOf(jwt.getSubject()),
				Perfil.valueOf(jwt.getClaimAsString(TokenService.CLAIM_PERFIL)));
	}

	public boolean isCuidador() {
		return perfil == Perfil.CUIDADOR;
	}

}
