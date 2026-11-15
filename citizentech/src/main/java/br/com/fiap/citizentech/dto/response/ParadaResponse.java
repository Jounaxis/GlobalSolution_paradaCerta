package br.com.fiap.citizentech.dto.response;

import java.math.BigDecimal;

import br.com.fiap.citizentech.model.Parada;

public record ParadaResponse(
		Long id,
		Long codigoSptrans,
		String nome,
		String endereco,
		BigDecimal latitude,
		BigDecimal longitude) {

	public static ParadaResponse from(Parada parada) {
		return new ParadaResponse(
				parada.getId(),
				parada.getCodigoSptrans(),
				parada.getNome(),
				parada.getEndereco(),
				parada.getLatitude(),
				parada.getLongitude());
	}

}
