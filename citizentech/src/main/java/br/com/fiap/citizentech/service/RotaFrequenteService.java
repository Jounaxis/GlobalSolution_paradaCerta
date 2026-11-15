package br.com.fiap.citizentech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.RotaFrequenteRequest;
import br.com.fiap.citizentech.dto.response.RotaFrequenteResponse;
import br.com.fiap.citizentech.exception.ConflitoException;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.model.RotaFrequente;
import br.com.fiap.citizentech.repository.RotaFrequenteRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RotaFrequenteService {

	private static final String MSG_DUPLICADA = "O passageiro ja tem uma rota frequente com essa parada e essa linha";

	private final RotaFrequenteRepository rotaFrequenteRepository;
	private final PassageiroService passageiroService;
	private final ParadaService paradaService;
	private final LinhaService linhaService;

	/** Lista todas as rotas ou, se o passageiro for informado, so as dele. */
	public Page<RotaFrequenteResponse> listar(Long passageiroId, Pageable pageable) {
		Page<RotaFrequente> pagina = passageiroId != null
				? rotaFrequenteRepository.findByPassageiroId(passageiroId, pageable)
				: rotaFrequenteRepository.findAll(pageable);
		return pagina.map(RotaFrequenteResponse::from);
	}

	public RotaFrequenteResponse buscarPorId(Long id) {
		return RotaFrequenteResponse.from(buscarEntidade(id));
	}

	@Transactional
	public RotaFrequenteResponse criar(RotaFrequenteRequest request) {
		if (rotaFrequenteRepository.existsByPassageiroIdAndParadaIdAndLinhaId(
				request.passageiroId(), request.paradaId(), request.linhaId())) {
			throw new ConflitoException(MSG_DUPLICADA);
		}
		RotaFrequente rota = new RotaFrequente();
		preencher(rota, request);
		return RotaFrequenteResponse.from(rotaFrequenteRepository.save(rota));
	}

	@Transactional
	public RotaFrequenteResponse atualizar(Long id, RotaFrequenteRequest request) {
		RotaFrequente rota = buscarEntidade(id);
		if (rotaFrequenteRepository.existsByPassageiroIdAndParadaIdAndLinhaIdAndIdNot(
				request.passageiroId(), request.paradaId(), request.linhaId(), id)) {
			throw new ConflitoException(MSG_DUPLICADA);
		}
		preencher(rota, request);
		return RotaFrequenteResponse.from(rota);
	}

	@Transactional
	public void excluir(Long id) {
		rotaFrequenteRepository.delete(buscarEntidade(id));
	}

	public RotaFrequente buscarEntidade(Long id) {
		return rotaFrequenteRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Rota frequente", id));
	}

	private void preencher(RotaFrequente rota, RotaFrequenteRequest request) {
		rota.setPassageiro(passageiroService.buscarEntidade(request.passageiroId()));
		rota.setParada(paradaService.buscarEntidade(request.paradaId()));
		rota.setLinha(linhaService.buscarEntidade(request.linhaId()));
		rota.setApelido(request.apelido());
	}

}
