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

import br.com.fiap.citizentech.dto.request.LinhaRequest;
import br.com.fiap.citizentech.dto.response.LinhaResponse;
import br.com.fiap.citizentech.service.LinhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/linhas")
@RequiredArgsConstructor
@Tag(name = "Linhas", description = "Linhas de onibus")
public class LinhaController {

	private final LinhaService linhaService;

	@GetMapping
	@Operation(summary = "Lista as linhas (paginado)")
	public Page<LinhaResponse> listar(@ParameterObject @PageableDefault(size = 10, sort = "letreiro") Pageable pageable) {
		return linhaService.listar(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca uma linha pelo id")
	public LinhaResponse buscarPorId(@PathVariable Long id) {
		return linhaService.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra uma linha")
	public ResponseEntity<LinhaResponse> criar(@RequestBody @Valid LinhaRequest request,
			UriComponentsBuilder uriBuilder) {
		LinhaResponse criada = linhaService.criar(request);
		URI uri = uriBuilder.path("/api/linhas/{id}").buildAndExpand(criada.id()).toUri();
		return ResponseEntity.created(uri).body(criada);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza uma linha")
	public LinhaResponse atualizar(@PathVariable Long id, @RequestBody @Valid LinhaRequest request) {
		return linhaService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui uma linha")
	public void excluir(@PathVariable Long id) {
		linhaService.excluir(id);
	}

}
