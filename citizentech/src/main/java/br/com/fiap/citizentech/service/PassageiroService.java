package br.com.fiap.citizentech.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.PassageiroRequest;
import br.com.fiap.citizentech.dto.response.PassageiroResponse;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.model.Passageiro;
import br.com.fiap.citizentech.model.enums.Perfil;
import br.com.fiap.citizentech.repository.PassageiroRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassageiroService {

	private final PassageiroRepository passageiroRepository;
	private final UsuarioService usuarioService;

	/** Lista todos os passageiros ou, se o cuidador for informado, so os dele. */
	public Page<PassageiroResponse> listar(Long cuidadorId, Pageable pageable) {
		Page<Passageiro> pagina = cuidadorId != null
				? passageiroRepository.findByCuidadorId(cuidadorId, pageable)
				: passageiroRepository.findAll(pageable);
		return pagina.map(PassageiroResponse::from);
	}

	public PassageiroResponse buscarPorId(Long id) {
		return PassageiroResponse.from(buscarEntidade(id));
	}

	@Transactional
	public PassageiroResponse criar(PassageiroRequest request) {
		Passageiro passageiro = new Passageiro();
		preencher(passageiro, request);
		passageiro.setConsentimentoEm(LocalDateTime.now());
		return PassageiroResponse.from(passageiroRepository.save(passageiro));
	}

	@Transactional
	public PassageiroResponse atualizar(Long id, PassageiroRequest request) {
		Passageiro passageiro = buscarEntidade(id);
		boolean novaVersaoDoTermo = !passageiro.getVersaoTermo().equals(request.versaoTermo().trim());
		preencher(passageiro, request);
		if (novaVersaoDoTermo) {
			// LGPD: aceite de uma nova versao do termo registra um novo consentimento
			passageiro.setConsentimentoEm(LocalDateTime.now());
		}
		return PassageiroResponse.from(passageiro);
	}

	@Transactional
	public void excluir(Long id) {
		passageiroRepository.delete(buscarEntidade(id));
	}

	public Passageiro buscarEntidade(Long id) {
		return passageiroRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Passageiro", id));
	}

	private void preencher(Passageiro passageiro, PassageiroRequest request) {
		passageiro.setCuidador(usuarioService.buscarComPerfil(request.cuidadorId(), Perfil.CUIDADOR));
		passageiro.setNome(request.nome().trim());
		passageiro.setTipoNecessidade(request.tipoNecessidade());
		passageiro.setContatoEmergencia(request.contatoEmergencia().trim());
		passageiro.setVersaoTermo(request.versaoTermo().trim());
	}

}
