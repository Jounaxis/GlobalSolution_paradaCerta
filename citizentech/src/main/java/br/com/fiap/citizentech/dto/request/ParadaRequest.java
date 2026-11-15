package br.com.fiap.citizentech.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ParadaRequest(

		@NotNull(message = "O codigo SPTrans e obrigatorio")
		@Positive(message = "O codigo SPTrans deve ser positivo")
		@Digits(integer = 10, fraction = 0, message = "O codigo SPTrans deve ter ate 10 digitos")
		Long codigoSptrans,

		@NotBlank(message = "O nome e obrigatorio")
		@Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
		String nome,

		@Size(max = 250, message = "O endereco deve ter no maximo 250 caracteres")
		String endereco,

		@DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90")
		@DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90")
		@Digits(integer = 3, fraction = 6, message = "A latitude aceita ate 6 casas decimais")
		BigDecimal latitude,

		@DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180")
		@DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180")
		@Digits(integer = 3, fraction = 6, message = "A longitude aceita ate 6 casas decimais")
		BigDecimal longitude) {
}
