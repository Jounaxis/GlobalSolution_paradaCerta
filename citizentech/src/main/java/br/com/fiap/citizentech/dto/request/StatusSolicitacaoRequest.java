package br.com.fiap.citizentech.dto.request;

import br.com.fiap.citizentech.model.enums.StatusSolicitacao;
import jakarta.validation.constraints.NotNull;

public record StatusSolicitacaoRequest(

		@NotNull(message = "O novo status e obrigatorio")
		StatusSolicitacao status) {
}
