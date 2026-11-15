package br.com.fiap.citizentech.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RotaFrequenteRequest(

		@NotNull(message = "O passageiro e obrigatorio")
		Long passageiroId,

		@NotNull(message = "A parada e obrigatoria")
		Long paradaId,

		@NotNull(message = "A linha e obrigatoria")
		Long linhaId,

		@Size(max = 60, message = "O apelido deve ter no maximo 60 caracteres")
		String apelido) {
}
