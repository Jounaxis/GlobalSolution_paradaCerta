package br.com.fiap.citizentech.exception;

/** Dado duplicado ou que conflita com um registro existente. Vira HTTP 409. */
public class ConflitoException extends RuntimeException {

	public ConflitoException(String mensagem) {
		super(mensagem);
	}

}
