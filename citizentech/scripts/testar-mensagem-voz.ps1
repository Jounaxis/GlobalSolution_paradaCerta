param(
    [ValidateRange(1, 65535)]
    [int]$Porta = 18080
)

$ErrorActionPreference = 'Stop'
$api = "http://127.0.0.1:$Porta"
$login = Invoke-RestMethod -Method Post -Uri "$api/auth/login" -ContentType 'application/json' -Body (@{
    email = 'gestor@paradacerta.com'
    senha = 'Gestor@123'
} | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.token)" }
$totens = Invoke-RestMethod -Uri "$api/api/totens" -Headers $headers
$totem = $totens.content | Where-Object identificador -EQ 'TOTEM-0001' | Select-Object -First 1
if (-not $totem) { throw 'TOTEM-0001 ausente. Use um banco novo do perfil dev.' }

$pedido = @{
    totemId = $totem.id
    tipoSinal = 'BOTAO'
    chaveIdempotencia = "voz-dev-$([guid]::NewGuid().ToString('N'))"
} | ConvertTo-Json
$tempo = [System.Diagnostics.Stopwatch]::StartNew()
$resposta = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$api/api/solicitacoes" `
    -Headers $headers -ContentType 'application/json' -Body $pedido -TimeoutSec 60
$tempo.Stop()
$criada = $resposta.Content | ConvertFrom-Json
if ($resposta.StatusCode -ne 201 -or [string]::IsNullOrWhiteSpace($criada.mensagemVoz)) {
    throw 'Esperava HTTP 201 com uma mensagem de voz.'
}
$gravada = Invoke-RestMethod -Uri "$api/api/solicitacoes/$($criada.id)" -Headers $headers
$bytesUtf8 = [System.Text.Encoding]::UTF8.GetByteCount($criada.mensagemVoz)
if ($gravada.mensagemVoz -cne $criada.mensagemVoz -or $bytesUtf8 -gt 300) {
    throw 'A mensagem deve ser persistida e ter no maximo 300 bytes UTF-8.'
}

[pscustomobject]@{
    http = $resposta.StatusCode
    solicitacaoId = $criada.id
    statusNaCriacao = $criada.status
    statusNaConsulta = $gravada.status
    mensagemVoz = $criada.mensagemVoz
    caracteres = $criada.mensagemVoz.Length
    bytesUtf8 = $bytesUtf8
    duracaoPostMs = $tempo.ElapsedMilliseconds
    persistida = $true
    geracaoMensagemVoz = $criada.geracaoMensagemVoz
} | ConvertTo-Json -Depth 5
