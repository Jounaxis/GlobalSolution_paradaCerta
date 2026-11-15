package br.com.fiap.citizentech.exception;

/** Recurso buscado pelo id nao existe. Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String recurso, Long id) {
		super(recurso + " com id " + id + " nao encontrado(a)");
	}

}
