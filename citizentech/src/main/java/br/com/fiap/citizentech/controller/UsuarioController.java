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

import br.com.fiap.citizentech.dto.request.UsuarioRequest;
import br.com.fiap.citizentech.dto.response.UsuarioResponse;
import br.com.fiap.citizentech.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Cuidadores, motoristas e gestores")
public class UsuarioController {

	private final UsuarioService usuarioService;

	@GetMapping
	@Operation(summary = "Lista os usuarios (paginado)")
	public Page<UsuarioResponse> listar(@ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
		return usuarioService.listar(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um usuario pelo id")
	public UsuarioResponse buscarPorId(@PathVariable Long id) {
		return usuarioService.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra um usuario")
	public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid UsuarioRequest request,
			UriComponentsBuilder uriBuilder) {
		UsuarioResponse criado = usuarioService.criar(request);
		URI uri = uriBuilder.path("/api/usuarios/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(uri).body(criado);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza um usuario")
	public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody @Valid UsuarioRequest request) {
		return usuarioService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui um usuario")
	public void excluir(@PathVariable Long id) {
		usuarioService.excluir(id);
	}

}
