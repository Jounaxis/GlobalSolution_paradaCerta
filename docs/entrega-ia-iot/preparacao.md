# Preparação da entrega de IA e IoT

- Repositório base (`upstream`): [Malualvess-dev/GlobalSolution_CitizenTech](https://github.com/Malualvess-dev/GlobalSolution_CitizenTech).
- Fork da entrega (`origin`): [Jounaxis/GlobalSolution_paradaCerta](https://github.com/Jounaxis/GlobalSolution_paradaCerta).
- Base: [`c48a1ae22d5479cbffcfe0258b7fa5f18bfded48`](https://github.com/Malualvess-dev/GlobalSolution_CitizenTech/commit/c48a1ae22d5479cbffcfe0258b7fa5f18bfded48), conferida no `main` remoto em 17/11/2026.
- Branch da entrega: `entrega-ia-iot`.
- Instruções locais: o repositório base e os diretórios da cópia foram conferidos. O `README.md` orienta o uso do Maven Wrapper e do perfil `dev`.

## Escopo e fontes

O enunciado da disciplina pede IA generativa integrada, uma técnica de aprofundamento justificada, avaliação reproduzível e documentação de arquitetura e execução. O formato de submissão no Portal do Aluno é um `.txt` com os RMs, os nomes completos e o link do GitHub.

A equipe escolheu aplicar RAG às mensagens de voz. A documentação do Parada Certa fundamenta o fluxo de embarque, o RF11, o RNF07 e a reprodução de voz no simulador (p. 4–8 e 14–16). O roteiro desta entrega segue os cerca de 12 casos solicitados. O DDL de referência especifica `SOLICITACAO_EMBARQUE.MENSAGEM_VOZ` como `VARCHAR2(300)`.

## Fluxo atual

`POST /auth/login` emite o JWT. O `POST /api/solicitacoes` exige o perfil `GESTOR` e gera o texto quando o campo `mensagemVoz` está ausente ou vazio. O `MensagemVozService` chama a OpenAI via Spring AI 2.0.1, com `OPENAI_MODEL` configurável e `gpt-4o-mini` como padrão. A exceção do cliente ou a resposta vazia aciona a frase padrão local. O texto é persistido e pode ser conferido com `GET /api/solicitacoes/{id}`.

A geração acontece com a solicitação em `CRIADA`, antes do processamento do alerta ao motorista. O contexto atual informa necessidade, linha, destino, parada e tipo de sinal. Ele não inclui status ou previsão. O prompt atual menciona ações futuras do motorista e anúncios da parada que precisam ser revistos antes da avaliação final.

## Arquivos previstos

| Etapa | Arquivos |
| --- | --- |
| Preparação e chamada inicial | `preparacao.md`, `citizentech/scripts/testar-mensagem-voz.ps1` e `ai/MensagemVozService.java` para identificar a origem no log |
| RAG | Arquivos de orientação em `src/main/resources`, recuperação textual e testes no pacote `ai` |
| Fluxo de embarque | `ai/ContextoMensagemVoz.java`, `ai/MensagemVozService.java`, `service/SolicitacaoEmbarqueService.java`, contrato de resposta e testes da solicitação |
| Simulador | Página de desenvolvimento, recursos estáticos e rota no perfil `dev`, usando o login e a API existentes |
| Avaliação e entrega | Casos e resultados em `docs/entrega-ia-iot`, `README.md` e o `.txt` de identificação quando os dados estiverem disponíveis |

## Reproduzir a verificação inicial

No Windows, execute na pasta `citizentech`. O perfil `dev` usa H2 em memória, mensageria local e dados fictícios criados na inicialização. O bloco abaixo aproveita o JDK 21 e o cache do Maven Wrapper preparados ao lado desta cópia. Em outro checkout, configure `JAVA_HOME` para um JDK 21 local.

```powershell
$opcoesMaven = @()
if ((Test-Path '..\..\jdk-21.0.13+9') -and (Test-Path '..\..\maven\repository')) {
    $env:JAVA_HOME = (Resolve-Path '..\..\jdk-21.0.13+9').Path
    $env:MAVEN_USER_HOME = (Resolve-Path '..\..\maven').Path
    $repositorioMaven = Join-Path $env:MAVEN_USER_HOME 'repository'
    $opcoesMaven = @('-o', "-Dmaven.repo.local=$repositorioMaven")
}
```

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '18080'
$env:LOGGING_LEVEL_BR_COM_FIAP_CITIZENTECH_AI = 'DEBUG'
$env:OPENAI_MODEL = 'gpt-4o-mini'
$chaveApi = Read-Host 'Chave da API OpenAI' -AsSecureString
$env:OPENAI_API_KEY = [pscredential]::new('openai', $chaveApi).GetNetworkCredential().Password
Remove-Variable chaveApi
try {
    .\mvnw.cmd @opcoesMaven spring-boot:run
} finally {
    Remove-Item Env:OPENAI_API_KEY -ErrorAction SilentlyContinue
}
```

Em outro PowerShell, na mesma pasta `citizentech`, execute `.\scripts\testar-mensagem-voz.ps1`. O teste usa o JWT do gestor fictício, cria um pedido pelo botão sem passageiro nem linha e confere se a API devolve HTTP 201 e persiste a mesma mensagem. O tempo exibido cobre o `POST` completo.

Confira o log da API imediatamente após a solicitação. `origem=MODELO` marca o texto retornado pela chamada ao modelo. `origem=CONTINGENCIA` indica que a API usou a frase padrão. O HTTP 201 confirma a criação do pedido em ambos os casos.

Para testar a contingência de forma controlada, use um PowerShell novo com as mesmas opções de perfil, porta e log. Configure as variáveis abaixo e execute o Maven Wrapper. A porta local 9 deve estar sem servidor. Rode o mesmo script e confira `origem=CONTINGENCIA`.

```powershell
$env:OPENAI_API_KEY = 'chave-ficticia'
$env:SPRING_AI_OPENAI_BASE_URL = 'http://127.0.0.1:9'
$env:SPRING_AI_OPENAI_CHAT_MAX_RETRIES = '0'
```

## Resultado em 17/11/2026

- Ambiente validado com Temurin 21.0.13 e Maven 3.10.0 via Wrapper. Perfil `dev` com H2 e aviso local.
- Suíte automatizada executada offline. 25 testes aprovados, incluindo testes do texto com respostas controladas e testes de integração no perfil `test`.
- Contingência executada pelo endpoint autenticado com o provedor apontando para uma porta local sem servidor. HTTP 201, ID 1, status `CRIADA` na criação e na leitura, mensagem persistida, 93 caracteres e 432 ms para o `POST`. O log mostrou `origem=CONTINGENCIA motivo=OpenAIIoException`. O fluxo de aviso registrou que nenhum motorista foi avisado porque o pedido estava sem linha.
- Texto registrado: “Pedido de embarque registrado. Aguarde na parada, o motorista do proximo onibus sera avisado.” A promessa de aviso no texto é uma limitação da versão base para este cenário.
- Modelo real pendente. O host respondeu HTTP 401 a uma verificação de conectividade sem credencial. A geração com `gpt-4o-mini` exige uma chave válida com acesso ao modelo.
