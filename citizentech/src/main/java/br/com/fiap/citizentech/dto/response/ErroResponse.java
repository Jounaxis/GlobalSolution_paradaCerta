package br.com.fiap.citizentech.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Formato padrao de todas as respostas de erro da API.
 * A lista de campos so aparece em erros de validacao.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponse(
		LocalDateTime timestamp,
		int status,
		String erro,
		String mensagem,
		String caminho,
		List<CampoInvalido> campos) {

	public record CampoInvalido(String campo, String mensagem) {
	}

}
