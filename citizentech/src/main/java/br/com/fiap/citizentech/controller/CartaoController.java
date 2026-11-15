package br.com.fiap.citizentech.controller;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

import br.com.fiap.citizentech.dto.request.CartaoRequest;
import br.com.fiap.citizentech.dto.response.CartaoResponse;
import br.com.fiap.citizentech.security.UsuarioLogado;
import br.com.fiap.citizentech.service.CartaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cartoes")
@RequiredArgsConstructor
@Tag(name = "Cartoes", description = "Bilhetes Unicos vinculados aos passageiros")
public class CartaoController {

	private final CartaoService cartaoService;

	@GetMapping
	@Operation(summary = "Lista os cartoes (paginado), com filtro opcional por passageiro")
	public Page<CartaoResponse> listar(
			@Parameter(description = "Filtra pelos cartoes deste passageiro") @RequestParam(required = false) Long passageiroId,
			@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable,
			@AuthenticationPrincipal Jwt jwt) {
		return cartaoService.listar(passageiroId, pageable, UsuarioLogado.from(jwt));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um cartao pelo id")
	public CartaoResponse buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		return cartaoService.buscarPorId(id, UsuarioLogado.from(jwt));
	}

	@PostMapping
	@Operation(summary = "Vincula um cartao a um passageiro (o UID e gravado como hash)")
	public ResponseEntity<CartaoResponse> criar(@RequestBody @Valid CartaoRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		CartaoResponse criado = cartaoService.criar(request, UsuarioLogado.from(jwt));
		URI uri = uriBuilder.path("/api/cartoes/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(uri).body(criado);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza um cartao (troca de passageiro, UID ou ativacao)")
	public CartaoResponse atualizar(@PathVariable Long id, @RequestBody @Valid CartaoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return cartaoService.atualizar(id, request, UsuarioLogado.from(jwt));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui um cartao")
	public void excluir(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		cartaoService.excluir(id, UsuarioLogado.from(jwt));
	}

}
