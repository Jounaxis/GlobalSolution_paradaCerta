package br.com.fiap.citizentech.dto.response;

import java.time.LocalDateTime;

import br.com.fiap.citizentech.model.TentativaEmbarque;
import br.com.fiap.citizentech.model.Usuario;
import br.com.fiap.citizentech.model.Veiculo;
import br.com.fiap.citizentech.model.enums.ResultadoTentativa;

public record TentativaEmbarqueResponse(
		Long id,
		Long solicitacaoId,
		Long linhaId,
		String linhaLetreiro,
		Long veiculoId,
		String veiculoPrefixo,
		Long motoristaId,
		String motoristaNome,
		LocalDateTime previsaoChegada,
		LocalDateTime alertadoEm,
		ResultadoTentativa resultado,
		LocalDateTime confirmadoEm) {

	public static TentativaEmbarqueResponse from(TentativaEmbarque tentativa) {
		Veiculo veiculo = tentativa.getVeiculo();
		Usuario motorista = tentativa.getMotorista();
		return new TentativaEmbarqueResponse(
				tentativa.getId(),
				tentativa.getSolicitacao().getId(),
				tentativa.getLinha().getId(),
				tentativa.getLinha().getLetreiro(),
				veiculo != null ? veiculo.getId() : null,
				veiculo != null ? veiculo.getPrefixo() : null,
				motorista != null ? motorista.getId() : null,
				motorista != null ? motorista.getNome() : null,
				tentativa.getPrevisaoChegada(),
				tentativa.getAlertadoEm(),
				tentativa.getResultado(),
				tentativa.getConfirmadoEm());
	}

}
