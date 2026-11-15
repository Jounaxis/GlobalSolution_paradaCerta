package br.com.fiap.citizentech.exception;

/** Requisicao valida no formato, mas que quebra uma regra de negocio. Vira HTTP 422. */
public class RegraNegocioException extends RuntimeException {

	public RegraNegocioException(String mensagem) {
		super(mensagem);
	}

}
