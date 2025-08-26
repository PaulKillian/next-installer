package com.example.nextinstaller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.github.github.GitHubClient
import com.example.github.termux.TermuxRunner



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun App() {
        var tab by remember { mutableIntStateOf(0) }
        Scaffold(topBar = { TopAppBar(title = { Text("Next Installer") }) }) { pad ->
            Column(Modifier.padding(pad).padding(16.dp)) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Next.js Setup") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("GitHub Repo") })
                }
                Spacer(Modifier.height(12.dp))
                if (tab == 0) NextSetupTab() else RepoTab()
            }
        }
    }

    @Composable
    fun NextSetupTab() {
        var project by remember { mutableStateOf("myapp") }
        var useTS by remember { mutableStateOf(true) }
        var useTailwind by remember { mutableStateOf(true) }
        var useAppRouter by remember { mutableStateOf(true) }
        var useESLint by remember { mutableStateOf(true) }
        var usePNPM by remember { mutableStateOf(true) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(project, { project = it }, label = { Text("Project name") })

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = useTS, onClick = { useTS = !useTS }, label = { Text("TypeScript") })
                FilterChip(selected = useTailwind, onClick = { useTailwind = !useTailwind }, label = { Text("Tailwind") })
                FilterChip(selected = useAppRouter, onClick = { useAppRouter = !useAppRouter }, label = { Text("App Router") })
                FilterChip(selected = useESLint, onClick = { useESLint = !useESLint }, label = { Text("ESLint") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(onClick = { usePNPM = true }, label = { Text("pnpm") })
                AssistChip(onClick = { usePNPM = false }, label = { Text("npm") })
                Text(if (usePNPM) "Using pnpm" else "Using npm")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { ensureTermuxOrOpen() }) { Text("Install Termux") }
                Button(onClick = { runPrereqs(usePNPM) }) { Text("Install prerequisites") }
            }

            Button(onClick = {
                val flags = buildString {
                    if (useTS) append(" --ts")
                    if (useESLint) append(" --eslint")
                    if (useAppRouter) append(" --app")
                    if (useTailwind) append(" --tailwind")
                    append(" --git=false")
                }
                val pkgMgrInit = if (usePNPM) "corepack enable && corepack prepare pnpm@latest --activate" else ":"
                val install = if (usePNPM) "pnpm install" else "npm install"
                val script = """
                    set -e
                    pkg update -y && pkg upgrade -y
                    pkg install -y git nodejs-lts
                    $pkgMgrInit
                    cd ~
                    npx create-next-app@latest ${'$'}project$flags
                    cd ${'$'}project
                    echo 'NEXT_TELEMETRY_DISABLED=1' >> .env.local
                    $install
                    echo "Done. Start dev server with: ${if (usePNPM) "pnpm dev" else "npm run dev"}"
                """.trimIndent()
                TermuxRunner.runCommand(this@MainActivity, script = script, background = false)
            }) { Text("Create Next.js app") }

            Text("When ready, open http://127.0.0.1:3000 in your Android browser.")
        }
    }

    private fun ensureTermuxOrOpen() {
        if (!TermuxRunner.isInstalled(this)) TermuxRunner.openInstallPage(this)
    }

    private fun runPrereqs(usePNPM: Boolean) {
        val pkgMgrInit = if (usePNPM) "corepack enable && corepack prepare pnpm@latest --activate" else ":"
        val script = """
            set -e
            pkg update -y && pkg upgrade -y
            pkg install -y git nodejs-lts
            $pkgMgrInit
        """.trimIndent()
        TermuxRunner.runCommand(this, script = script, background = true)
    }

    @Composable
    fun RepoTab() {
        var repo by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isPrivate by remember { mutableStateOf(false) }
        var token by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("") }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(repo, { repo = it }, label = { Text("Repository name") })
            OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") })
            Row {
                Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                Spacer(Modifier.width(8.dp))
                Text("Private repo")
            }
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("GitHub Personal Access Token") },
                visualTransformation = PasswordVisualTransformation()
            )

            Button(onClick = {
                status = "Creating repo…"
                GitHubClient.createRepoAsync(
                    token = token,
                    name = repo,
                    description = description,
                    privateRepo = isPrivate
                ) { ok, msg -> status = msg }
            }) { Text("Create GitHub repo") }

            if (status.isNotBlank()) Text(status)

            Divider()
            Text("Optional: push from Termux after repo exists")
            Button(onClick = {
                val url = "https://github.com/<your-username>/${'$'}repo.git"
                val script = """
                    set -e
                    cd ~/${'$'}repo || cd ~
                    if [ ! -d "${'$'}repo/.git" ]; then
                      cd ~/${'$'}repo
                      git init -b main
                    fi
                    git remote remove origin 2>/dev/null || true
                    git remote add origin "$url"
                    git add -A && git commit -m "chore: first push" || true
                    git push -u origin main
                """.trimIndent()
                TermuxRunner.runCommand(this@MainActivity, script = script, background = false)
            }) { Text("Push from Termux") }
        }
    }
}
