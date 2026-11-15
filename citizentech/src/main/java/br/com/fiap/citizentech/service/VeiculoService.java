package br.com.fiap.citizentech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.VeiculoRequest;
import br.com.fiap.citizentech.dto.response.VeiculoResponse;
import br.com.fiap.citizentech.exception.ConflitoException;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.model.Veiculo;
import br.com.fiap.citizentech.model.enums.Perfil;
import br.com.fiap.citizentech.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VeiculoService {

	private final VeiculoRepository veiculoRepository;
	private final LinhaService linhaService;
	private final UsuarioService usuarioService;

	public Page<VeiculoResponse> listar(Pageable pageable) {
		return veiculoRepository.findAll(pageable).map(VeiculoResponse::from);
	}

	public VeiculoResponse buscarPorId(Long id) {
		return VeiculoResponse.from(buscarEntidade(id));
	}

	@Transactional
	public VeiculoResponse criar(VeiculoRequest request) {
		String prefixo = request.prefixo().trim();
		if (veiculoRepository.existsByPrefixo(prefixo)) {
			throw new ConflitoException("Ja existe um veiculo com o prefixo " + prefixo);
		}
		if (request.motoristaId() != null && veiculoRepository.existsByMotoristaId(request.motoristaId())) {
			throw new ConflitoException("O motorista " + request.motoristaId() + " ja esta em outro veiculo");
		}
		Veiculo veiculo = new Veiculo();
		preencher(veiculo, request, prefixo);
		return VeiculoResponse.from(veiculoRepository.save(veiculo));
	}

	@Transactional
	public VeiculoResponse atualizar(Long id, VeiculoRequest request) {
		Veiculo veiculo = buscarEntidade(id);
		String prefixo = request.prefixo().trim();
		if (veiculoRepository.existsByPrefixoAndIdNot(prefixo, id)) {
			throw new ConflitoException("Ja existe um veiculo com o prefixo " + prefixo);
		}
		if (request.motoristaId() != null
				&& veiculoRepository.existsByMotoristaIdAndIdNot(request.motoristaId(), id)) {
			throw new ConflitoException("O motorista " + request.motoristaId() + " ja esta em outro veiculo");
		}
		preencher(veiculo, request, prefixo);
		return VeiculoResponse.from(veiculo);
	}

	@Transactional
	public void excluir(Long id) {
		veiculoRepository.delete(buscarEntidade(id));
	}

	public Veiculo buscarEntidade(Long id) {
		return veiculoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo", id));
	}

	private void preencher(Veiculo veiculo, VeiculoRequest request, String prefixo) {
		veiculo.setPrefixo(prefixo);
		veiculo.setLinha(linhaService.buscarEntidade(request.linhaId()));
		veiculo.setMotorista(request.motoristaId() != null
				? usuarioService.buscarComPerfil(request.motoristaId(), Perfil.MOTORISTA)
				: null);
	}

}
