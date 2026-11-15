package br.com.fiap.citizentech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.LinhaRequest;
import br.com.fiap.citizentech.dto.response.LinhaResponse;
import br.com.fiap.citizentech.exception.ConflitoException;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.model.Linha;
import br.com.fiap.citizentech.repository.LinhaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinhaService {

	private final LinhaRepository linhaRepository;

	public Page<LinhaResponse> listar(Pageable pageable) {
		return linhaRepository.findAll(pageable).map(LinhaResponse::from);
	}

	public LinhaResponse buscarPorId(Long id) {
		return LinhaResponse.from(buscarEntidade(id));
	}

	@Transactional
	public LinhaResponse criar(LinhaRequest request) {
		if (linhaRepository.existsByCodigoSptrans(request.codigoSptrans())) {
			throw new ConflitoException("Ja existe uma linha com o codigo SPTrans " + request.codigoSptrans());
		}
		Linha linha = new Linha();
		preencher(linha, request);
		return LinhaResponse.from(linhaRepository.save(linha));
	}

	@Transactional
	public LinhaResponse atualizar(Long id, LinhaRequest request) {
		Linha linha = buscarEntidade(id);
		if (linhaRepository.existsByCodigoSptransAndIdNot(request.codigoSptrans(), id)) {
			throw new ConflitoException("Ja existe uma linha com o codigo SPTrans " + request.codigoSptrans());
		}
		preencher(linha, request);
		return LinhaResponse.from(linha);
	}

	@Transactional
	public void excluir(Long id) {
		linhaRepository.delete(buscarEntidade(id));
	}

	public Linha buscarEntidade(Long id) {
		return linhaRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Linha", id));
	}

	private void preencher(Linha linha, LinhaRequest request) {
		linha.setCodigoSptrans(request.codigoSptrans());
		linha.setLetreiro(request.letreiro().trim());
		linha.setSentido(request.sentido());
		linha.setDestino(request.destino());
	}

}
