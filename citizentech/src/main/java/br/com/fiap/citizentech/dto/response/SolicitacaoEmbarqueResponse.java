package br.com.fiap.citizentech.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.fiap.citizentech.ai.ResultadoMensagemVoz.Detalhes;
import br.com.fiap.citizentech.model.Linha;
import br.com.fiap.citizentech.model.Passageiro;
import br.com.fiap.citizentech.model.SolicitacaoEmbarque;
import br.com.fiap.citizentech.model.enums.StatusSolicitacao;
import br.com.fiap.citizentech.model.enums.TipoSinal;

public record SolicitacaoEmbarqueResponse(
		Long id,
		Long passageiroId,
		String passageiroNome,
		Long totemId,
		String totemIdentificador,
		Long linhaId,
		String linhaLetreiro,
		TipoSinal tipoSinal,
		StatusSolicitacao status,
		String chaveIdempotencia,
		String mensagemVoz,
		LocalDateTime criadaEm,
		LocalDateTime atualizadaEm,
		@JsonInclude(JsonInclude.Include.NON_NULL) Detalhes geracaoMensagemVoz) {

	public static SolicitacaoEmbarqueResponse from(SolicitacaoEmbarque solicitacao) {
		return from(solicitacao, null);
	}

	public static SolicitacaoEmbarqueResponse from(SolicitacaoEmbarque solicitacao, Detalhes geracaoMensagemVoz) {
		Passageiro passageiro = solicitacao.getPassageiro();
		Linha linha = solicitacao.getLinha();
		return new SolicitacaoEmbarqueResponse(
				solicitacao.getId(),
				passageiro != null ? passageiro.getId() : null,
				passageiro != null ? passageiro.getNome() : null,
				solicitacao.getTotem().getId(),
				solicitacao.getTotem().getIdentificador(),
				linha != null ? linha.getId() : null,
				linha != null ? linha.getLetreiro() : null,
				solicitacao.getTipoSinal(),
				solicitacao.getStatus(),
				solicitacao.getChaveIdempotencia(),
				solicitacao.getMensagemVoz(),
				solicitacao.getCriadaEm(),
				solicitacao.getAtualizadaEm(),
				geracaoMensagemVoz);
	}

}
