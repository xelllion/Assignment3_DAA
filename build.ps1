$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
    New-Item -ItemType Directory -Path out -Force | Out-Null
    $sources = @(Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName })
    & $javac --release 17 -encoding UTF-8 -d out $sources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    Write-Output 'Build successful. Run: java -cp out Main'
} finally {
    Pop-Location
}
