package br.com.fiap.citizentech.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import br.com.fiap.citizentech.security.JwtProperties;
import br.com.fiap.citizentech.security.SecurityErrorHandler;
import br.com.fiap.citizentech.security.TokenService;

/**
 * Seguranca da API com JWT (stateless).
 * <p>
 * Papeis: ROLE_CUIDADOR, ROLE_MOTORISTA e ROLE_GESTOR, vindos do claim "perfil" do token.
 * <ul>
 * <li>Publico: login, registro de cuidador, Swagger e health check.</li>
 * <li>GESTOR: usuarios, cadastro de paradas, linhas, totens e veiculos, e exclusao de solicitacoes.</li>
 * <li>CUIDADOR e GESTOR: passageiros, cartoes e rotas frequentes (o cuidador so ve os dele).</li>
 * <li>MOTORISTA e GESTOR: consulta de solicitacoes e mudanca de status.</li>
 * <li>Qualquer usuario logado: consulta de paradas e linhas.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

	private static final String ROLE_PREFIX = "ROLE_";
	private static final String GESTOR = "GESTOR";
	private static final String CUIDADOR = "CUIDADOR";
	private static final String MOTORISTA = "MOTORISTA";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler securityErrorHandler,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		return http
				// API REST stateless com token no header: sem sessao e sem CSRF
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/auth/login", "/auth/registro").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/actuator/health/**", "/actuator/info", "/error").permitAll()

						.requestMatchers("/api/usuarios/**").hasRole(GESTOR)

						.requestMatchers(HttpMethod.GET, "/api/paradas/**", "/api/linhas/**").authenticated()
						.requestMatchers("/api/paradas/**", "/api/linhas/**", "/api/totens/**").hasRole(GESTOR)

						.requestMatchers(HttpMethod.GET, "/api/veiculos/**").hasAnyRole(MOTORISTA, GESTOR)
						.requestMatchers("/api/veiculos/**").hasRole(GESTOR)

						.requestMatchers("/api/passageiros/**", "/api/cartoes/**", "/api/rotas-frequentes/**")
						.hasAnyRole(CUIDADOR, GESTOR)

						.requestMatchers(HttpMethod.GET, "/api/solicitacoes/**").hasAnyRole(MOTORISTA, GESTOR)
						.requestMatchers(HttpMethod.PATCH, "/api/solicitacoes/*/status").hasAnyRole(MOTORISTA, GESTOR)
						.requestMatchers("/api/solicitacoes/**").hasRole(GESTOR)

						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint(securityErrorHandler)
						.accessDeniedHandler(securityErrorHandler))
				.exceptionHandling(erros -> erros
						.authenticationEntryPoint(securityErrorHandler)
						.accessDeniedHandler(securityErrorHandler))
				.build();
	}

	/** Le o claim "perfil" do token e transforma em papel: GESTOR -> ROLE_GESTOR. */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
		papeis.setAuthoritiesClaimName(TokenService.CLAIM_PERFIL);
		papeis.setAuthorityPrefix(ROLE_PREFIX);

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(papeis);
		return converter;
	}

	@Bean
	public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chave(jwtProperties)));
	}

	/** Valida assinatura, expiracao e emissor de todo token recebido. */
	@Bean
	public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(jwtProperties))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));
		return decoder;
	}

	private SecretKey chave(JwtProperties jwtProperties) {
		return new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
