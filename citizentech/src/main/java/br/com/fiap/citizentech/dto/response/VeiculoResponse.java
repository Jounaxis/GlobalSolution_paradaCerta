package br.com.fiap.citizentech.dto.response;

import br.com.fiap.citizentech.model.Usuario;
import br.com.fiap.citizentech.model.Veiculo;

public record VeiculoResponse(
		Long id,
		String prefixo,
		Long linhaId,
		String linhaLetreiro,
		Long motoristaId,
		String motoristaNome) {

	public static VeiculoResponse from(Veiculo veiculo) {
		Usuario motorista = veiculo.getMotorista();
		return new VeiculoResponse(
				veiculo.getId(),
				veiculo.getPrefixo(),
				veiculo.getLinha().getId(),
				veiculo.getLinha().getLetreiro(),
				motorista != null ? motorista.getId() : null,
				motorista != null ? motorista.getNome() : null);
	}

}
