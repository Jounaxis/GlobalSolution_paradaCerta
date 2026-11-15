package br.com.fiap.citizentech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.ParadaRequest;
import br.com.fiap.citizentech.dto.response.ParadaResponse;
import br.com.fiap.citizentech.exception.ConflitoException;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.model.Parada;
import br.com.fiap.citizentech.repository.ParadaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParadaService {

	private final ParadaRepository paradaRepository;

	public Page<ParadaResponse> listar(Pageable pageable) {
		return paradaRepository.findAll(pageable).map(ParadaResponse::from);
	}

	public ParadaResponse buscarPorId(Long id) {
		return ParadaResponse.from(buscarEntidade(id));
	}

	@Transactional
	public ParadaResponse criar(ParadaRequest request) {
		if (paradaRepository.existsByCodigoSptrans(request.codigoSptrans())) {
			throw new ConflitoException("Ja existe uma parada com o codigo SPTrans " + request.codigoSptrans());
		}
		Parada parada = new Parada();
		preencher(parada, request);
		return ParadaResponse.from(paradaRepository.save(parada));
	}

	@Transactional
	public ParadaResponse atualizar(Long id, ParadaRequest request) {
		Parada parada = buscarEntidade(id);
		if (paradaRepository.existsByCodigoSptransAndIdNot(request.codigoSptrans(), id)) {
			throw new ConflitoException("Ja existe uma parada com o codigo SPTrans " + request.codigoSptrans());
		}
		preencher(parada, request);
		return ParadaResponse.from(parada);
	}

	@Transactional
	public void excluir(Long id) {
		paradaRepository.delete(buscarEntidade(id));
	}

	public Parada buscarEntidade(Long id) {
		return paradaRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Parada", id));
	}

	private void preencher(Parada parada, ParadaRequest request) {
		parada.setCodigoSptrans(request.codigoSptrans());
		parada.setNome(request.nome().trim());
		parada.setEndereco(request.endereco());
		parada.setLatitude(request.latitude());
		parada.setLongitude(request.longitude());
	}

}
