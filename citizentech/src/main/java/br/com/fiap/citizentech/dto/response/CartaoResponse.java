package br.com.fiap.citizentech.dto.response;

import java.time.LocalDateTime;

import br.com.fiap.citizentech.model.Cartao;

/** O hash do UID nao e devolvido: ele so serve para a busca interna. */
public record CartaoResponse(
		Long id,
		Long passageiroId,
		String passageiroNome,
		Boolean ativo,
		LocalDateTime criadoEm) {

	public static CartaoResponse from(Cartao cartao) {
		return new CartaoResponse(
				cartao.getId(),
				cartao.getPassageiro().getId(),
				cartao.getPassageiro().getNome(),
				cartao.getAtivo(),
				cartao.getCriadoEm());
	}

}
