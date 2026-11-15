package br.com.fiap.citizentech.controller;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.fiap.citizentech.dto.request.RotaFrequenteRequest;
import br.com.fiap.citizentech.dto.response.RotaFrequenteResponse;
import br.com.fiap.citizentech.service.RotaFrequenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rotas-frequentes")
@RequiredArgsConstructor
@Tag(name = "Rotas frequentes", description = "Parada de origem + linha que o passageiro costuma usar")
public class RotaFrequenteController {

	private final RotaFrequenteService rotaFrequenteService;

	@GetMapping
	@Operation(summary = "Lista as rotas frequentes (paginado), com filtro opcional por passageiro")
	public Page<RotaFrequenteResponse> listar(
			@Parameter(description = "Filtra pelas rotas deste passageiro") @RequestParam(required = false) Long passageiroId,
			@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return rotaFrequenteService.listar(passageiroId, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca uma rota frequente pelo id")
	public RotaFrequenteResponse buscarPorId(@PathVariable Long id) {
		return rotaFrequenteService.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra uma rota frequente")
	public ResponseEntity<RotaFrequenteResponse> criar(@RequestBody @Valid RotaFrequenteRequest request,
			UriComponentsBuilder uriBuilder) {
		RotaFrequenteResponse criada = rotaFrequenteService.criar(request);
		URI uri = uriBuilder.path("/api/rotas-frequentes/{id}").buildAndExpand(criada.id()).toUri();
		return ResponseEntity.created(uri).body(criada);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza uma rota frequente")
	public RotaFrequenteResponse atualizar(@PathVariable Long id, @RequestBody @Valid RotaFrequenteRequest request) {
		return rotaFrequenteService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui uma rota frequente")
	public void excluir(@PathVariable Long id) {
		rotaFrequenteService.excluir(id);
	}

}
