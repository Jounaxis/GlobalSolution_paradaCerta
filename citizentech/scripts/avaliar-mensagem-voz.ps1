# Requer PowerShell 7. Utilize apenas as instâncias de dados fictícios do perfil dev.
param(
    [ValidateRange(1, 65535)][int]$Porta = 18082,
    [ValidateRange(1, 65535)][int]$PortaSemRag = 18084,
    [ValidateRange(1, 65535)][int]$PortaFalha = 18083,
    [string]$Saida
)

$ErrorActionPreference = 'Stop'
$ollama = 'http://127.0.0.1:11434'
$modelo = 'qwen3:4b-instruct-2507-q4_K_M'

function Chamar-Api($sessao, [string]$rota, [string]$metodo = 'Get', $corpo) {
    $opcoes = @{
        Uri = "$($sessao.base)$rota"
        Method = $metodo
        TimeoutSec = 20
    }
    if ($sessao.headers) { $opcoes.Headers = $sessao.headers }
    if ($null -ne $corpo) {
        $opcoes.ContentType = 'application/json; charset=utf-8'
        $opcoes.Body = $corpo | ConvertTo-Json -Depth 5 -Compress
    }
    $http = Invoke-WebRequest @opcoes
    [pscustomobject]@{ status = [int]$http.StatusCode; dados = $http.Content | ConvertFrom-Json }
}

function Abrir-Sessao([int]$portaApi) {
    $sessao = @{ base = "http://127.0.0.1:$portaApi" }
    $login = Chamar-Api $sessao '/auth/login' 'Post' @{ email = 'gestor@paradacerta.com'; senha = 'Gestor@123' }
    $sessao.headers = @{ Authorization = "Bearer $($login.dados.token)" }
    $sessao.totem = (Chamar-Api $sessao '/api/totens').dados.content | Where-Object identificador -EQ 'TOTEM-0001' | Select-Object -First 1
    $sessao.cuidador = (Chamar-Api $sessao '/api/usuarios').dados.content | Where-Object email -EQ 'cuidador@paradacerta.com' | Select-Object -First 1
    $sessao.linhas = @{}
    foreach ($linha in (Chamar-Api $sessao '/api/linhas?size=100').dados.content) {
        $sessao.linhas[$linha.letreiro] = $linha
    }
    if (-not $sessao.totem -or -not $sessao.cuidador) { throw "Dados de demonstração ausentes na porta $portaApi." }
    return $sessao
}

function Estado-Modelo($manter) {
    Invoke-RestMethod -Method Post -Uri "$ollama/api/generate" -ContentType 'application/json' `
        -Body (@{ model = $modelo; keep_alive = $manter } | ConvertTo-Json) -TimeoutSec 60 | Out-Null
}

function Testar-Pedido($sessao, $caso, [bool]$comRag) {
    $entrada = @{
        totemId = $sessao.totem.id
        tipoSinal = $caso.sinal
        chaveIdempotencia = "avaliacao-$([guid]::NewGuid().ToString('N'))"
    }
    $linha = $null
    if ($caso.linha) {
        $linha = $sessao.linhas[$caso.linha]
        if (-not $linha) { throw "Linha do caso $($caso.id) ausente na porta $($sessao.base)." }
        $entrada.linhaId = $linha.id
    }
    if ($caso.necessidade) {
        $pessoa = Chamar-Api $sessao '/api/passageiros' 'Post' @{
            cuidadorId = $sessao.cuidador.id; nome = "Passageiro fictício avaliação $($caso.id)"
            tipoNecessidade = $caso.necessidade; contatoEmergencia = '00000000000'; consentimento = $true; versaoTermo = 'demo'
        }
        $entrada.passageiroId = $pessoa.dados.id
    }
    if ($caso.textoLegado) { $entrada.mensagemVoz = $caso.textoLegado }

    $tempo = [System.Diagnostics.Stopwatch]::StartNew()
    $criada = Chamar-Api $sessao '/api/solicitacoes' 'Post' $entrada
    $tempo.Stop()
    $pedido = $criada.dados
    $gravada = (Chamar-Api $sessao "/api/solicitacoes/$($pedido.id)").dados
    $referencias = @($pedido.geracaoMensagemVoz.referencias)
    $ids = @($referencias | ForEach-Object id)
    $esperadas = if ($comRag) { $caso.refs }
    $frases = @($pedido.mensagemVoz -split '(?<=\.)\s+')
    $dadosLinha = ''
    if ($linha) { $dadosLinha = " para a linha $($linha.letreiro)" }
    if ($linha.destino) { $dadosLinha += ", sentido $($linha.destino)" }
    $anuncioEsperado = "Pedido de embarque registrado$dadosLinha."
    $esperaClara = $frases.Count -eq 2 -and $frases[1] -match '^(Por favor, )?(Aguarde|Espere)( com calma)? (na parada|perto do totem)\.$'
    $bytes = [Text.Encoding]::UTF8.GetByteCount($pedido.mensagemVoz)
    $statusEsperado = if ($linha) { 'MOTORISTA_AVISADO' } else { 'CRIADA' }
    $textoEsperado = if ($linha) { "Aviso de embarque registrado$dadosLinha. $($frases[1])" } else { $pedido.mensagemVoz }
    $estadoFinal = $null
    if ($caso.encerrar) {
        $alterada = (Chamar-Api $sessao "/api/solicitacoes/$($pedido.id)/status" 'Patch' @{ status = 'EMBARCADO' }).dados
        $releitura = (Chamar-Api $sessao "/api/solicitacoes/$($pedido.id)").dados
        $estadoFinal = @{ status = $releitura.status; mensagem = $releitura.mensagemVoz }
        $estadoCorreto = $releitura.status -eq 'EMBARCADO' -and $releitura.mensagemVoz -ceq "Embarque confirmado$dadosLinha." `
            -and $releitura.mensagemVoz -ceq $alterada.mensagemVoz
    } else {
        $estadoCorreto = $true
    }
    $origem = $pedido.geracaoMensagemVoz.origem
    $criterios = [ordered]@{
        recuperacao = ($ids -join ',') -ceq ($esperadas -join ',')
        fidelidade = $pedido.status -eq 'CRIADA' -and $frases[0] -ceq $anuncioEsperado -and $esperaClara
        clareza = $esperaClara
        tamanho = $frases.Count -le 2 -and $bytes -le 300
        persistencia = $gravada.status -eq $statusEsperado -and $gravada.mensagemVoz -ceq $textoEsperado -and $estadoCorreto
        origem = if ($caso.falha) { $origem -eq 'CONTINGENCIA' } else { $origem -in @('MODELO', 'CONTINGENCIA') }
    }

    [pscustomobject][ordered]@{
        id = $caso.id
        caso = $caso.nome
        rag = $comRag
        entrada = [ordered]@{
            sinal = $caso.sinal; necessidade = $caso.necessidade; linha = $linha.letreiro; destino = $linha.destino
            previsao = $null; mensagemLegada = $caso.textoLegado; condicao = $caso.falha
        }
        http = $criada.status
        solicitacaoId = $pedido.id
        statusNaCriacao = $pedido.status
        origem = $origem
        referenciasEsperadas = @($esperadas | Where-Object { $_ })
        referenciasRecuperadas = $referencias
        mensagem = $pedido.mensagemVoz
        frases = $frases.Count
        caracteres = $pedido.mensagemVoz.Length
        bytesUtf8 = $bytes
        duracaoPostMs = $tempo.ElapsedMilliseconds
        leitura = @{ status = $gravada.status; mensagem = $gravada.mensagemVoz }
        encerramento = $estadoFinal
        criterios = $criterios
        aprovado = $criada.status -eq 201 -and $criterios.Values -notcontains $false
    }
}

$principal = Abrir-Sessao $Porta
$semRag = Abrir-Sessao $PortaSemRag
$falha = Abrir-Sessao $PortaFalha
$foraDaBase = Chamar-Api $principal '/api/linhas' 'Post' @{
    codigoSptrans = Get-Random -Minimum 760700000 -Maximum 760799999
    letreiro = '042X-10'; destino = 'Terminal Horizonte'; sentido = 1
}
$principal.linhas['042X-10'] = $foraDaBase.dados

$casos = @(
    @{ id = '01'; nome = 'Botão sem identificação'; sinal = 'BOTAO'; refs = @('PC-03', 'PC-01', 'PC-02') }
    @{ id = '02'; nome = 'Cartão de baixa visão'; sinal = 'CARTAO'; necessidade = 'BAIXA_VISAO'; linha = '6091-10'; refs = @('PC-01', 'PC-04') }
    @{ id = '03'; nome = 'Cartão de cadeira de rodas'; sinal = 'CARTAO'; necessidade = 'CADEIRA_DE_RODAS'; linha = '6091-10'; refs = @('PC-01', 'PC-05') }
    @{ id = '04'; nome = 'Mobilidade reduzida sem destino'; sinal = 'CARTAO'; necessidade = 'MOBILIDADE_REDUZIDA'; linha = '7012-10'; refs = @('PC-01', 'PC-03', 'PC-06') }
    @{ id = '05'; nome = 'Cartão de pessoa idosa'; sinal = 'CARTAO'; necessidade = 'IDOSO'; linha = '875A-10'; refs = @('PC-01', 'PC-07') }
    @{ id = '06'; nome = 'Botão com linha confirmada'; sinal = 'BOTAO'; linha = '6091-10'; refs = @('PC-01', 'PC-02') }
    @{ id = '07'; nome = 'Baixa visão sem linha'; sinal = 'CARTAO'; necessidade = 'BAIXA_VISAO'; refs = @('PC-03', 'PC-01', 'PC-04') }
    @{ id = '08'; nome = 'Linha e destino fora do corpus de orientações'; sinal = 'BOTAO'; linha = '042X-10'; refs = @('PC-01', 'PC-02') }
    @{ id = '09'; nome = 'Texto legado fora do corpus com previsão inventada'; sinal = 'BOTAO'; textoLegado = 'Saldo do cartão: vinte reais. O ônibus chega em 4 minutos.'; refs = @('PC-03', 'PC-01', 'PC-02') }
    @{ id = '10'; nome = 'Modelo descarregado'; sinal = 'BOTAO'; falha = 'tempo_limite'; refs = @('PC-03', 'PC-01', 'PC-02') }
    @{ id = '11'; nome = 'Endpoint do provedor indisponível'; sinal = 'BOTAO'; falha = 'provedor_indisponivel'; refs = @('PC-03', 'PC-01', 'PC-02') }
    @{ id = '12'; nome = 'Aviso e confirmação de embarque'; sinal = 'CARTAO'; necessidade = 'CADEIRA_DE_RODAS'; linha = '6091-10'; encerrar = $true; refs = @('PC-01', 'PC-05') }
)

Estado-Modelo '10m'
$resultados = foreach ($caso in $casos) {
    $sessao = $principal
    if ($caso.id -eq '10') {
        Estado-Modelo 0
        if ((Invoke-RestMethod -Uri "$ollama/api/ps").models.Count -ne 0) { throw 'O modelo ainda está carregado.' }
    }
    if ($caso.id -eq '11') { $sessao = $falha }
    if ($caso.id -eq '12') { Estado-Modelo '10m' }
    Testar-Pedido $sessao $caso $true
}
$comparacao = foreach ($caso in $casos | Where-Object { $_.id -in @('01', '02', '04') }) {
    Testar-Pedido $semRag $caso $false
}

$json = [pscustomobject][ordered]@{
    executadoEm = Get-Date -Format o
    modelo = $modelo
    versaoOllama = (Invoke-RestMethod -Uri "$ollama/api/version").version
    prazoGeracaoMs = 3000
    comRag = @($resultados)
    semRag = @($comparacao)
} | ConvertTo-Json -Depth 10
if ($Saida) { $json | Set-Content -LiteralPath $Saida -Encoding utf8 } else { $json }
