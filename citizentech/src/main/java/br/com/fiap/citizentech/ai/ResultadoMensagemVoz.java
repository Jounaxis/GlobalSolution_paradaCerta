package br.com.fiap.citizentech.ai;

import java.util.List;

public record ResultadoMensagemVoz(String texto, Detalhes detalhes) {

	public record Detalhes(Origem origem, List<Referencia> referencias) {
	}

	public record Referencia(String id, String assunto, String origem) {
	}

	public enum Origem {
		MODELO,
		CONTINGENCIA
	}
}
