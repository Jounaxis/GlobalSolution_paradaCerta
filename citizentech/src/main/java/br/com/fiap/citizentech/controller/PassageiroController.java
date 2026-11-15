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

import br.com.fiap.citizentech.dto.request.PassageiroRequest;
import br.com.fiap.citizentech.dto.response.PassageiroResponse;
import br.com.fiap.citizentech.service.PassageiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/passageiros")
@RequiredArgsConstructor
@Tag(name = "Passageiros", description = "Passageiros assistidos, cadastrados pelos cuidadores")
public class PassageiroController {

	private final PassageiroService passageiroService;

	@GetMapping
	@Operation(summary = "Lista os passageiros (paginado), com filtro opcional por cuidador")
	public Page<PassageiroResponse> listar(
			@Parameter(description = "Filtra pelos passageiros deste cuidador") @RequestParam(required = false) Long cuidadorId,
			@ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
		return passageiroService.listar(cuidadorId, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um passageiro pelo id")
	public PassageiroResponse buscarPorId(@PathVariable Long id) {
		return passageiroService.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cadastra um passageiro (exige o aceite do termo de consentimento)")
	public ResponseEntity<PassageiroResponse> criar(@RequestBody @Valid PassageiroRequest request,
			UriComponentsBuilder uriBuilder) {
		PassageiroResponse criado = passageiroService.criar(request);
		URI uri = uriBuilder.path("/api/passageiros/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(uri).body(criado);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza um passageiro")
	public PassageiroResponse atualizar(@PathVariable Long id, @RequestBody @Valid PassageiroRequest request) {
		return passageiroService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui um passageiro (cartoes e rotas sao apagados juntos; solicitacoes ficam anonimas)")
	public void excluir(@PathVariable Long id) {
		passageiroService.excluir(id);
	}

}
