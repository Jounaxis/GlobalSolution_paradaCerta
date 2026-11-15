package br.com.fiap.citizentech.controller;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.fiap.citizentech.dto.request.SolicitacaoEmbarqueRequest;
import br.com.fiap.citizentech.dto.request.StatusSolicitacaoRequest;
import br.com.fiap.citizentech.dto.response.AuditoriaSolicitacaoResponse;
import br.com.fiap.citizentech.dto.response.SolicitacaoEmbarqueResponse;
import br.com.fiap.citizentech.dto.response.TentativaEmbarqueResponse;
import br.com.fiap.citizentech.model.enums.StatusSolicitacao;
import br.com.fiap.citizentech.service.SolicitacaoEmbarqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/solicitacoes")
@RequiredArgsConstructor
@Tag(name = "Solicitacoes de embarque", description = "Pedidos de embarque feitos nos totens, tentativas e auditoria")
public class SolicitacaoEmbarqueController {

	private final SolicitacaoEmbarqueService solicitacaoService;

	@GetMapping
	@Operation(summary = "Lista as solicitacoes (paginado, mais recentes primeiro), com filtro opcional por status")
	public Page<SolicitacaoEmbarqueResponse> listar(
			@Parameter(description = "Filtra pelas solicitacoes neste status") @RequestParam(required = false) StatusSolicitacao status,
			@ParameterObject @PageableDefault(size = 10, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
		return solicitacaoService.listar(status, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca uma solicitacao pelo id")
	public SolicitacaoEmbarqueResponse buscarPorId(@PathVariable Long id) {
		return solicitacaoService.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Abre uma solicitacao de embarque")
	public ResponseEntity<SolicitacaoEmbarqueResponse> criar(@RequestBody @Valid SolicitacaoEmbarqueRequest request,
			UriComponentsBuilder uriBuilder) {
		SolicitacaoEmbarqueResponse criada = solicitacaoService.criar(request);
		URI uri = uriBuilder.path("/api/solicitacoes/{id}").buildAndExpand(criada.id()).toUri();
		return ResponseEntity.created(uri).body(criada);
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Muda o status de uma solicitacao em andamento")
	public SolicitacaoEmbarqueResponse atualizarStatus(@PathVariable Long id,
			@RequestBody @Valid StatusSolicitacaoRequest request) {
		return solicitacaoService.atualizarStatus(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui uma solicitacao (a auditoria do Oracle e mantida)")
	public void excluir(@PathVariable Long id) {
		solicitacaoService.excluir(id);
	}

	@GetMapping("/{id}/tentativas")
	@Operation(summary = "Lista as tentativas de atendimento da solicitacao, em ordem de alerta")
	public List<TentativaEmbarqueResponse> listarTentativas(@PathVariable Long id) {
		return solicitacaoService.listarTentativas(id);
	}

	@GetMapping("/{id}/auditoria")
	@Operation(summary = "Lista o historico de alteracoes da solicitacao, gravado pelo gatilho do Oracle")
	public List<AuditoriaSolicitacaoResponse> listarAuditoria(@PathVariable Long id) {
		return solicitacaoService.listarAuditoria(id);
	}

}
