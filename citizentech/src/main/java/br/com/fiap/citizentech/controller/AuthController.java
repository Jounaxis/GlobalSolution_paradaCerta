package br.com.fiap.citizentech.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.fiap.citizentech.dto.request.LoginRequest;
import br.com.fiap.citizentech.dto.request.RegistroRequest;
import br.com.fiap.citizentech.dto.response.LoginResponse;
import br.com.fiap.citizentech.dto.response.UsuarioResponse;
import br.com.fiap.citizentech.security.UsuarioLogado;
import br.com.fiap.citizentech.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Login com JWT e cadastro de cuidadores pelo app")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	@SecurityRequirements
	@Operation(summary = "Faz login e devolve o token JWT")
	public LoginResponse login(@RequestBody @Valid LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/registro")
	@SecurityRequirements
	@Operation(summary = "Cadastro publico pelo app (cria sempre um CUIDADOR)")
	public ResponseEntity<UsuarioResponse> registrar(@RequestBody @Valid RegistroRequest request,
			UriComponentsBuilder uriBuilder) {
		UsuarioResponse criado = authService.registrar(request);
		URI uri = uriBuilder.path("/api/usuarios/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(uri).body(criado);
	}

	@GetMapping("/me")
	@Operation(summary = "Dados do usuario dono do token")
	public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
		return authService.usuarioAtual(UsuarioLogado.from(jwt).id());
	}

}
