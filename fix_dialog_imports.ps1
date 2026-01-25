
$files = @(
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\components\dialog\MusicPlayerDialog.kt",
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\components\dialog\TaskFlowDialog.kt",
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\components\dialog\ResourceInstallDialog.kt"
)

$componentImports = @{
    "ShardDialog" = "import com.lanrhyme.shardlauncher.ui.components.basic.ShardDialog"
    "ShardButton" = "import com.lanrhyme.shardlauncher.ui.components.basic.ShardButton"
    "ShardCard" = "import com.lanrhyme.shardlauncher.ui.components.basic.ShardCard"
    "PopupContainer" = "import com.lanrhyme.shardlauncher.ui.components.basic.PopupContainer"
    "TitleAndSummary" = "import com.lanrhyme.shardlauncher.ui.components.basic.TitleAndSummary"
    "ShardDropdownMenu" = "import com.lanrhyme.shardlauncher.ui.components.basic.ShardDropdownMenu"
    "SearchTextField" = "import com.lanrhyme.shardlauncher.ui.components.basic.SearchTextField"
    "selectableCard" = "import com.lanrhyme.shardlauncher.ui.components.basic.selectableCard"
    "SwitchLayoutCard" = "import com.lanrhyme.shardlauncher.ui.components.layout.SwitchLayoutCard"
    "SliderLayoutCard" = "import com.lanrhyme.shardlauncher.ui.components.layout.SliderLayoutCard"
}

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        $originalContent = $content
        $importsToAdd = @()
        
        foreach ($component in $componentImports.Keys) {
            if ($content.Contains($component) -and -not $content.Contains($componentImports[$component])) {
                $importsToAdd += $componentImports[$component]
            }
        }
        
        if ($importsToAdd.Count -gt 0) {
            $importBlock = $importsToAdd -join "`n"
            # Insert imports after the package declaration
            if ($content -match "(package .*`r?`n)") {
                $content = $content -replace "(package .*`r?`n)", "`$1`n$importBlock`n"
                Set-Content -Path $file -Value $content -Encoding UTF8
                Write-Host "Added $($importsToAdd.Count) imports to $file"
            }
        } else {
            Write-Host "No missing imports found in $file"
        }
    } else {
        Write-Host "File not found: $file"
    }
}
