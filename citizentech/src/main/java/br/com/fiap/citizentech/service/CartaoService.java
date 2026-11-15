package br.com.fiap.citizentech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.CartaoRequest;
import br.com.fiap.citizentech.dto.response.CartaoResponse;
import br.com.fiap.citizentech.exception.ConflitoException;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.model.Cartao;
import br.com.fiap.citizentech.repository.CartaoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartaoService {

	private final CartaoRepository cartaoRepository;
	private final PassageiroService passageiroService;
	private final HashService hashService;

	/** Lista todos os cartoes ou, se o passageiro for informado, so os dele. */
	public Page<CartaoResponse> listar(Long passageiroId, Pageable pageable) {
		Page<Cartao> pagina = passageiroId != null
				? cartaoRepository.findByPassageiroId(passageiroId, pageable)
				: cartaoRepository.findAll(pageable);
		return pagina.map(CartaoResponse::from);
	}

	public CartaoResponse buscarPorId(Long id) {
		return CartaoResponse.from(buscarEntidade(id));
	}

	@Transactional
	public CartaoResponse criar(CartaoRequest request) {
		String uidHash = hashService.hmacSha256(request.uid());
		if (cartaoRepository.existsByUidHash(uidHash)) {
			throw new ConflitoException("Este cartao ja esta vinculado a um passageiro");
		}
		Cartao cartao = new Cartao();
		preencher(cartao, request, uidHash);
		return CartaoResponse.from(cartaoRepository.save(cartao));
	}

	@Transactional
	public CartaoResponse atualizar(Long id, CartaoRequest request) {
		Cartao cartao = buscarEntidade(id);
		String uidHash = hashService.hmacSha256(request.uid());
		if (cartaoRepository.existsByUidHashAndIdNot(uidHash, id)) {
			throw new ConflitoException("Este cartao ja esta vinculado a um passageiro");
		}
		preencher(cartao, request, uidHash);
		return CartaoResponse.from(cartao);
	}

	@Transactional
	public void excluir(Long id) {
		cartaoRepository.delete(buscarEntidade(id));
	}

	public Cartao buscarEntidade(Long id) {
		return cartaoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Cartao", id));
	}

	private void preencher(Cartao cartao, CartaoRequest request, String uidHash) {
		cartao.setPassageiro(passageiroService.buscarEntidade(request.passageiroId()));
		cartao.setUidHash(uidHash);
		cartao.setAtivo(request.ativo() == null || request.ativo());
	}

}
