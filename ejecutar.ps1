param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Argumentos
)

$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $raiz '.env'
if (-not (Test-Path $envFile)) {
    Write-Error "Falta $envFile. Copiá .env.example y completá tus credenciales locales."
    exit 2
}

foreach ($linea in Get-Content $envFile) {
    $texto = $linea.Trim()
    if ($texto -eq '' -or $texto.StartsWith('#')) { continue }
    $separador = $texto.IndexOf('=')
    if ($separador -lt 1) { Write-Error "Línea inválida en .env (se esperaba CLAVE=valor)."; exit 2 }
    $clave = $texto.Substring(0, $separador).Trim()
    $valor = $texto.Substring($separador + 1).Trim()
    if ($valor.Length -ge 2 -and (($valor.StartsWith('"') -and $valor.EndsWith('"')) -or ($valor.StartsWith("'") -and $valor.EndsWith("'")))) {
        $valor = $valor.Substring(1, $valor.Length - 2)
    }
    Set-Item -Path "Env:$clave" -Value $valor
}

$jar = Join-Path $raiz 'target/banco-talentos-mail.jar'
if (-not (Test-Path $jar)) {
    Write-Error "Falta $jar. Ejecutá: mvn package"
    exit 1
}

$java = $env:JAVA_EXE
if (-not $java -and $env:JAVA_HOME) {
    $candidato = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (Test-Path $candidato) { $java = $candidato }
}
if (-not $java) {
    $java = Get-ChildItem 'C:\Program Files\Java\jdk-*\bin\java.exe' -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $java) { $java = 'java' }

& $java -jar $jar @Argumentos
exit $LASTEXITCODE
