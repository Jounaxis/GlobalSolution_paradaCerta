package br.com.fiap.citizentech.dto.response;

import br.com.fiap.citizentech.model.RotaFrequente;

public record RotaFrequenteResponse(
		Long id,
		Long passageiroId,
		String passageiroNome,
		Long paradaId,
		String paradaNome,
		Long linhaId,
		String linhaLetreiro,
		String apelido) {

	public static RotaFrequenteResponse from(RotaFrequente rota) {
		return new RotaFrequenteResponse(
				rota.getId(),
				rota.getPassageiro().getId(),
				rota.getPassageiro().getNome(),
				rota.getParada().getId(),
				rota.getParada().getNome(),
				rota.getLinha().getId(),
				rota.getLinha().getLetreiro(),
				rota.getApelido());
	}

}
