# Preparação da entrega de IA e IoT

- Repositório base (`upstream`): [Malualvess-dev/GlobalSolution_CitizenTech](https://github.com/Malualvess-dev/GlobalSolution_CitizenTech).
- Fork da entrega (`origin`): [Jounaxis/GlobalSolution_paradaCerta](https://github.com/Jounaxis/GlobalSolution_paradaCerta).
- Base: [`c48a1ae22d5479cbffcfe0258b7fa5f18bfded48`](https://github.com/Malualvess-dev/GlobalSolution_CitizenTech/commit/c48a1ae22d5479cbffcfe0258b7fa5f18bfded48), conferida no `main` remoto em 17/11/2026.
- Branch da entrega: `entrega-ia-iot`.
- Instruções locais: o repositório base e os diretórios da cópia foram conferidos. O `README.md` orienta o uso do Maven Wrapper e do perfil `dev`.

## Escopo e fontes

O enunciado da disciplina pede IA generativa integrada, uma técnica de aprofundamento justificada, avaliação reproduzível e documentação de arquitetura e execução. O formato de submissão no Portal do Aluno é um `.txt` com os RMs, os nomes completos e o link do GitHub.

A equipe escolheu aplicar RAG às mensagens de voz. A documentação do Parada Certa fundamenta o fluxo de embarque, o RF11, o RNF07 e a reprodução de voz no simulador (p. 4–8 e 14–16). O roteiro desta entrega segue os cerca de 12 casos solicitados. O DDL de referência especifica `SOLICITACAO_EMBARQUE.MENSAGEM_VOZ` como `VARCHAR2(300)`.

## Fluxo no commit base

`POST /auth/login` emite o JWT. O `POST /api/solicitacoes` exige o perfil `GESTOR` e gera o texto quando o campo `mensagemVoz` está ausente ou vazio. O `MensagemVozService` chama a OpenAI via Spring AI 2.0.1, com `OPENAI_MODEL` configurável e `gpt-4o-mini` como padrão. A exceção do cliente ou a resposta vazia aciona a frase padrão local. O texto é persistido e pode ser conferido com `GET /api/solicitacoes/{id}`.

A geração acontece com a solicitação em `CRIADA`, antes do processamento do alerta ao motorista. O contexto atual informa necessidade, linha, destino, parada e tipo de sinal. Ele não inclui status ou previsão. O prompt atual menciona ações futuras do motorista e anúncios da parada que precisam ser revistos antes da avaliação final.

## Arquivos previstos

| Etapa | Arquivos |
| --- | --- |
| Preparação e chamada inicial | `preparacao.md`, `citizentech/scripts/testar-mensagem-voz.ps1`, `application-ia-local.yaml` e `ai/MensagemVozService.java` para identificar a origem e falhas no log |
| RAG | Arquivos de orientação em `src/main/resources`, recuperação textual e testes no pacote `ai` |
| Fluxo de embarque | `ai/ContextoMensagemVoz.java`, `ai/MensagemVozService.java`, `service/SolicitacaoEmbarqueService.java`, contrato de resposta e testes da solicitação |
| Simulador | Página de desenvolvimento, recursos estáticos e rota no perfil `dev`, usando o login e a API existentes |
| Avaliação e entrega | Casos e resultados em `docs/entrega-ia-iot`, `README.md` e o `.txt` de identificação quando os dados estiverem disponíveis |

## Reproduzir a geração com o modelo local

O perfil `ia-local` usa a API compatível com OpenAI do [Ollama](https://docs.ollama.com/api/openai-compatibility), escutando por padrão em `127.0.0.1:11434`. O modelo é o [Qwen3 4B Instruct 2507 quantizado em Q4_K_M](https://ollama.com/library/qwen3:4b-instruct-2507-q4_K_M). O `MensagemVozService` mantém o `ChatClient` do Spring AI e o fluxo de criação e persistência da solicitação. O valor `ollama` do perfil serve para satisfazer o cliente HTTP e não é uma credencial.

Com o Ollama em execução no Windows, baixe o modelo uma vez. Depois, na pasta `citizentech`, configure `JAVA_HOME` para o JDK 21 e inicie a API.

```powershell
ollama pull qwen3:4b-instruct-2507-q4_K_M
$env:SPRING_PROFILES_ACTIVE = 'dev,ia-local'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '18082'
$env:LOGGING_LEVEL_BR_COM_FIAP_CITIZENTECH_AI = 'DEBUG'
.\mvnw.cmd spring-boot:run
```

Em outro PowerShell, execute `.\scripts\testar-mensagem-voz.ps1 -Porta 18082`. O teste autentica o gestor de exemplo, cria um pedido fictício pelo botão, consulta o pedido e compara o texto gravado. Confira HTTP 201, `persistida: true` e `origem=MODELO` no log. O mesmo teste com o Ollama indisponível deve registrar `origem=CONTINGENCIA`.

## Reproduzir a verificação inicial com a OpenAI

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
- A verificação inicial de conectividade com a OpenAI recebeu HTTP 401 sem credencial. A referência de geração para a entrega passou a ser o Qwen3 em execução local.

### Diagnóstico da chamada à OpenAI

O primeiro teste com a chave informada na sessão local retornou HTTP 201 no endpoint de embarque, gravou a frase padrão e levou 393 ms. O log marcou `origem=CONTINGENCIA motivo=IllegalArgumentException`. Esse teste confirma a persistência na contingência.

Em uma instância isolada, o mesmo fluxo usou uma chave fictícia ASCII para verificar se a chamada chegava ao provedor. A OpenAI recusou a chave com HTTP 401 (`UnauthorizedException`). O endpoint de embarque respondeu HTTP 201, persistiu a frase padrão e levou 1.083 ms.

Após iniciar a API com a nova chave, a chamada ao provedor retornou HTTP 429 (`RateLimitException`). A API Java respondeu HTTP 201, persistiu a contingência de 93 caracteres e levou 1.966 ms. A entrada foi o pedido fictício pelo botão, sem passageiro e sem linha selecionada.

O log passou a registrar os códigos de limite conhecidos do provedor. Uma repetição do mesmo cenário confirmou o código `credit_balance_exhausted`, com HTTP 201 no endpoint Java, persistência da contingência e 929 ms para o `POST`. A suíte automatizada continuou com 25 testes aprovados. O 429 motivou a escolha do modelo local para a avaliação.

### Geração real com o Qwen3 em 17/11/2026

O ambiente usou o Ollama portátil 0.40.16 e o artefato `qwen3:4b-instruct-2507-q4_K_M` (`0edcdef34593`). O servidor local escutou em `127.0.0.1`, com inferência em nuvem desativada, contexto de 4.096 tokens e aceleração Vulkan na AMD Radeon RX 7600. O Ollama confirmou HTTP 200 em `/v1/chat/completions` nas duas gerações. A suíte Maven separada passou com 25 testes.

O mesmo cenário fictício foi executado duas vezes pela API Java autenticada, com `tipoSinal=BOTAO`, sem passageiro identificado e sem linha. O cenário começou sem modelo carregado. Em ambos os pedidos a API respondeu HTTP 201, com status `CRIADA` e a mesma mensagem preservada no `GET`.

| Condição | Origem no log | ID | Caracteres | Tempo do `POST` |
| --- | --- | ---: | ---: | ---: |
| Primeira chamada, com carregamento do modelo | `MODELO` | 1 | 106 | 14.407 ms |
| Modelo já carregado | `MODELO` | 2 | 106 | 602 ms |

Texto gerado nas duas execuções:

> O seu pedido foi registrado. Fique perto do totem e aguarde o próximo ônibus na Av. Paulista, 1578 - MASP.

A mensagem confirmou o registro, citou a parada cadastrada e omitiu linha, destino e previsão. A primeira chamada ultrapassou os 3 segundos de referência da documentação do Parada Certa para acionar contingência. O teste mediu a versão base e mostrou que o prazo ainda não é aplicado no fluxo; a execução com o modelo carregado ficou abaixo dessa referência.

Em outra instância com `dev,ia-local`, foi configurada uma porta local sem servidor para simular o Ollama indisponível. O mesmo teste retornou HTTP 201 em 383 ms, fez a leitura da frase padrão de 93 caracteres e registrou `origem=CONTINGENCIA motivo=OpenAIIoException`. A frase padrão da versão base ainda promete aviso ao motorista quando falta a linha, como observado no teste de contingência inicial.
