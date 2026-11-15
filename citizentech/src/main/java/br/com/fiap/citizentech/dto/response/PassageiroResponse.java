package br.com.fiap.citizentech.dto.response;

import java.time.LocalDateTime;

import br.com.fiap.citizentech.model.Passageiro;
import br.com.fiap.citizentech.model.enums.TipoNecessidade;

public record PassageiroResponse(
		Long id,
		Long cuidadorId,
		String cuidadorNome,
		String nome,
		TipoNecessidade tipoNecessidade,
		String contatoEmergencia,
		LocalDateTime consentimentoEm,
		String versaoTermo,
		LocalDateTime criadoEm) {

	public static PassageiroResponse from(Passageiro passageiro) {
		return new PassageiroResponse(
				passageiro.getId(),
				passageiro.getCuidador().getId(),
				passageiro.getCuidador().getNome(),
				passageiro.getNome(),
				passageiro.getTipoNecessidade(),
				passageiro.getContatoEmergencia(),
				passageiro.getConsentimentoEm(),
				passageiro.getVersaoTermo(),
				passageiro.getCriadoEm());
	}

}
