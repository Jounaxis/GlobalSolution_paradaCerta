package br.com.fiap.citizentech.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LinhaRequest(

		@NotNull(message = "O codigo SPTrans e obrigatorio")
		@Positive(message = "O codigo SPTrans deve ser positivo")
		@Digits(integer = 10, fraction = 0, message = "O codigo SPTrans deve ter ate 10 digitos")
		Long codigoSptrans,

		@NotBlank(message = "O letreiro e obrigatorio")
		@Size(max = 20, message = "O letreiro deve ter no maximo 20 caracteres")
		String letreiro,

		@NotNull(message = "O sentido e obrigatorio")
		@Min(value = 1, message = "O sentido deve ser 1 ou 2")
		@Max(value = 2, message = "O sentido deve ser 1 ou 2")
		Integer sentido,

		@Size(max = 120, message = "O destino deve ter no maximo 120 caracteres")
		String destino) {
}
