
$files = @(
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\settings\OtherSettings.kt",
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\settings\GameSettings.kt",
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\settings\AboutScreen.kt",
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\downloads\VersionDetailScreen.kt",
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\developeroptions\ComponentDemoScreen.kt",
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\settings\LauncherSettings.kt",
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\home\HomeScreen.kt",
    "d:\AndroidStudioProjects\ShardLauncher\ShardLauncher\src\main\java\com\lanrhyme\shardlauncher\ui\downloads\GameDownloadContent.kt"
)

$replacements = @{
    "import com.lanrhyme.shardlauncher.ui.components.ScrollIndicator" = "import com.lanrhyme.shardlauncher.ui.components.basic.ScrollIndicator"
    "import com.lanrhyme.shardlauncher.ui.components.TitledDivider" = "import com.lanrhyme.shardlauncher.ui.components.basic.TitledDivider"
    "import com.lanrhyme.shardlauncher.ui.components.CapsuleTextField" = "import com.lanrhyme.shardlauncher.ui.components.basic.CapsuleTextField"
    "import com.lanrhyme.shardlauncher.ui.components.BackgroundTextTag" = "import com.lanrhyme.shardlauncher.ui.components.basic.BackgroundTextTag"
    "import com.lanrhyme.shardlauncher.ui.components.CombinedCard" = "import com.lanrhyme.shardlauncher.ui.components.basic.CombinedCard"
    "import com.lanrhyme.shardlauncher.ui.components.TitleAndSummary" = "import com.lanrhyme.shardlauncher.ui.components.basic.TitleAndSummary"
    "import com.lanrhyme.shardlauncher.ui.components.CollapsibleCard" = "import com.lanrhyme.shardlauncher.ui.components.basic.CollapsibleCard"
}

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        $originalContent = $content
        
        foreach ($key in $replacements.Keys) {
            if ($content.Contains($key)) {
                $content = $content.Replace($key, $replacements[$key])
                Write-Host "Updating $key in $file"
            }
        }
        
        if ($content -ne $originalContent) {
            Set-Content -Path $file -Value $content -Encoding UTF8
            Write-Host "Saved changes to $file"
        } else {
            Write-Host "No changes needed for $file"
        }
    } else {
        Write-Host "File not found: $file"
    }
}
