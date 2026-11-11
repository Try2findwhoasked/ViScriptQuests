param([switch]$Verify, [switch]$UiTest)
$ErrorActionPreference = 'Stop'
if (-not $env:VISCRIPT_WORKSPACE) { throw 'Set VISCRIPT_WORKSPACE in mise.local.toml to a ViScriptLib 1.20.1 parent workspace (see docs/BACKPORT_1_20_1.md).' }
$parent = (Resolve-Path -LiteralPath $env:VISCRIPT_WORKSPACE).Path
$wrapper = Join-Path $parent 'gradlew.bat'
if (-not (Test-Path -LiteralPath $wrapper)) { throw "Missing Gradle wrapper: $wrapper" }
$task = if ($UiTest) { ':ViScriptQuest:runClient' } elseif ($Verify) { ':ViScriptQuest:backportRegression' } else { ':ViScriptQuest:build' }
$extra = if ($UiTest) { @('-PldTest=quest_ai_adapter', '-PldTestWindow=1280x720') } else { @() }
$java21 = & mise where java@temurin-21.0.13+9.0.LTS
if ($LASTEXITCODE -ne 0) { throw 'Run mise install to install the Java 21 build-tool runtime.' }
& gradle.bat -p $parent $task --console=plain --no-configuration-cache "-Porg.gradle.java.installations.paths=$env:JAVA_HOME,$java21" @extra
exit $LASTEXITCODE
