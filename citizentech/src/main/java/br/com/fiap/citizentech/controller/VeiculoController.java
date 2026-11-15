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

import br.com.fiap.citizentech.dto.request.VeiculoRequest;
import br.com.fiap.citizentech.dto.response.VeiculoResponse;
import br.com.fiap.citizentech.service.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/veiculos")
@RequiredArgsConstructor
@Tag(name = "Veiculos", description = "Onibus, cada um com uma linha e um motorista")
public class VeiculoController {

	private final VeiculoService veiculoService;

	@GetMapping
	@Operation(summary = "Lista os veiculos (paginado)")
	public Page<VeiculoResponse> listar(@ParameterObject @PageableDefault(size = 10, sort = "prefixo") Pageable pageable) {
		return veiculoService.listar(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um veiculo pelo id")
	public VeiculoResponse buscarPorId(@PathVariable Long id) {
		return veiculoService.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra um veiculo")
	public ResponseEntity<VeiculoResponse> criar(@RequestBody @Valid VeiculoRequest request,
			UriComponentsBuilder uriBuilder) {
		VeiculoResponse criado = veiculoService.criar(request);
		URI uri = uriBuilder.path("/api/veiculos/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(uri).body(criado);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza um veiculo")
	public VeiculoResponse atualizar(@PathVariable Long id, @RequestBody @Valid VeiculoRequest request) {
		return veiculoService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui um veiculo")
	public void excluir(@PathVariable Long id) {
		veiculoService.excluir(id);
	}

}
