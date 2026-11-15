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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.fiap.citizentech.dto.request.ParadaRequest;
import br.com.fiap.citizentech.dto.response.ParadaResponse;
import br.com.fiap.citizentech.service.ParadaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/paradas")
@RequiredArgsConstructor
@Tag(name = "Paradas", description = "Paradas de onibus")
public class ParadaController {

	private final ParadaService paradaService;

	@GetMapping
	@Operation(summary = "Lista as paradas (paginado)")
	public Page<ParadaResponse> listar(@ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
		return paradaService.listar(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca uma parada pelo id")
	public ParadaResponse buscarPorId(@PathVariable Long id) {
		return paradaService.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra uma parada")
	public ResponseEntity<ParadaResponse> criar(@RequestBody @Valid ParadaRequest request,
			UriComponentsBuilder uriBuilder) {
		ParadaResponse criada = paradaService.criar(request);
		URI uri = uriBuilder.path("/api/paradas/{id}").buildAndExpand(criada.id()).toUri();
		return ResponseEntity.created(uri).body(criada);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza uma parada")
	public ParadaResponse atualizar(@PathVariable Long id, @RequestBody @Valid ParadaRequest request) {
		return paradaService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui uma parada")
	public void excluir(@PathVariable Long id) {
		paradaService.excluir(id);
	}

}
