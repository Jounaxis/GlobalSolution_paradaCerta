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

import br.com.fiap.citizentech.dto.request.TotemRequest;
import br.com.fiap.citizentech.dto.response.TotemResponse;
import br.com.fiap.citizentech.service.TotemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/totens")
@RequiredArgsConstructor
@Tag(name = "Totens", description = "Dispositivos ESP32 instalados nas paradas")
public class TotemController {

	private final TotemService totemService;

	@GetMapping
	@Operation(summary = "Lista os totens (paginado)")
	public Page<TotemResponse> listar(
			@ParameterObject @PageableDefault(size = 10, sort = "identificador") Pageable pageable) {
		return totemService.listar(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um totem pelo id")
	public TotemResponse buscarPorId(@PathVariable Long id) {
		return totemService.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra um totem (o token do dispositivo e gravado como hash)")
	public ResponseEntity<TotemResponse> criar(@RequestBody @Valid TotemRequest request,
			UriComponentsBuilder uriBuilder) {
		TotemResponse criado = totemService.criar(request);
		URI uri = uriBuilder.path("/api/totens/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(uri).body(criado);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza um totem (envie o token so se for troca-lo)")
	public TotemResponse atualizar(@PathVariable Long id, @RequestBody @Valid TotemRequest request) {
		return totemService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui um totem")
	public void excluir(@PathVariable Long id) {
		totemService.excluir(id);
	}

}
