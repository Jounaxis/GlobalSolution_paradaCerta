package br.com.fiap.citizentech.dto.response;

import br.com.fiap.citizentech.model.Linha;

public record LinhaResponse(
		Long id,
		Long codigoSptrans,
		String letreiro,
		Integer sentido,
		String destino) {

	public static LinhaResponse from(Linha linha) {
		return new LinhaResponse(
				linha.getId(),
				linha.getCodigoSptrans(),
				linha.getLetreiro(),
				linha.getSentido(),
				linha.getDestino());
	}

}
