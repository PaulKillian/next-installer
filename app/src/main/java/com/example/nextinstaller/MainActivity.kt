package com.example.nextinstaller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
          NextScaffolderScreen()
        }
      }
    }
  }
}

@Composable
fun NextScaffolderScreen() {
  val scope = rememberCoroutineScope()

  var repoName by remember { mutableStateOf("my-next-app") }
  var owner by remember { mutableStateOf("") }         // optional: target org/user for the new repo
  var runnerOwner by remember { mutableStateOf("") }   // owner of repo where the workflow lives (this repo)
  var runnerRepo by remember { mutableStateOf("") }    // repo name where the workflow lives (this repo)
  var token by remember { mutableStateOf("") }         // temporary: GitHub PAT to call repository_dispatch

  var typescript by remember { mutableStateOf(true) }
  var eslint by remember { mutableStateOf(true) }
  var tailwind by remember { mutableStateOf(true) }
  var srcDir by remember { mutableStateOf(true) }
  var appRouter by remember { mutableStateOf(true) }
  var importAlias by remember { mutableStateOf("@/*") }
  var pkgMgr by remember { mutableStateOf("pnpm") }    // npm | yarn | pnpm | bun
  var visibility by remember { mutableStateOf("private") } // private | public

  var busy by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("Next.js Project Generator", style = MaterialTheme.typography.headlineSmall)

    OutlinedTextField(
      value = repoName,
      onValueChange = { repoName = it },
      label = { Text("New repo name") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
      value = owner,
      onValueChange = { owner = it },
      label = { Text("Target owner (optional)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
      value = runnerOwner,
      onValueChange = { runnerOwner = it },
      label = { Text("Runner repo owner") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
      value = runnerRepo,
      onValueChange = { runnerRepo = it },
      label = { Text("Runner repo name") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
      value = token,
      onValueChange = { token = it },
      label = { Text("GitHub PAT (temporary)") },
      singleLine = true,
      visualTransformation = PasswordVisualTransformation(),
      modifier = Modifier.fillMaxWidth()
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Dropdown(
        value = pkgMgr,
        items = listOf("npm", "yarn", "pnpm", "bun"),
        onChange = { pkgMgr = it },
        label = "Package manager"
      )
      Spacer(Modifier.width(8.dp))
      Dropdown(
        value = visibility,
        items = listOf("private", "public"),
        onChange = { visibility = it },
        label = "Visibility"
      )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked = typescript, onCheckedChange = { typescript = it })
      Spacer(Modifier.width(6.dp)); Text("TypeScript")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked = eslint, onCheckedChange = { eslint = it })
      Spacer(Modifier.width(6.dp)); Text("ESLint")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked = tailwind, onCheckedChange = { tailwind = it })
      Spacer(Modifier.width(6.dp)); Text("Tailwind CSS")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked = srcDir, onCheckedChange = { srcDir = it })
      Spacer(Modifier.width(6.dp)); Text("Use /src dir")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked = appRouter, onCheckedChange = { appRouter = it })
      Spacer(Modifier.width(6.dp)); Text("App Router")
    }

    OutlinedTextField(
      value = importAlias,
      onValueChange = { importAlias = it },
      label = { Text("Import alias (optional)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    Button(
      enabled = !busy &&
        repoName.isNotBlank() &&
        runnerOwner.isNotBlank() &&
        runnerRepo.isNotBlank() &&
        token.isNotBlank(),
      onClick = {
        busy = true
        message = null
        scope.launch {
          val result = triggerDispatch(
            token = token.trim(),
            runnerOwner = runnerOwner.trim(),
            runnerRepo = runnerRepo.trim(),
            payload = mapOf(
              "repo_name" to repoName.trim(),
              "owner" to owner.trim(),       // optional
              "visibility" to visibility,
              "typescript" to typescript,
              "eslint" to eslint,
              "tailwind" to tailwind,
              "src_dir" to srcDir,
              "app_router" to appRouter,
              "import_alias" to importAlias.trim(),
              "pkg_manager" to pkgMgr
            )
          )
          message = result.fold(
            onSuccess = {
              val finalOwner = if (owner.isNotBlank()) owner.trim() else runnerOwner.trim()
              "Requested scaffold for \"$repoName\". " +
              "Check Actions in $runnerOwner/$runnerRepo. " +
              "New repo will be: https://github.com/$finalOwner/$repoName"
            },
            onFailure = { "Failed: ${it.message ?: "Unknown error"}" }
          )
          busy = false
        }
      }
    ) {
      if (busy) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
      } else {
        Text("Create Project")
      }
    }

    message?.let { Text(it) }
  }
}

@Composable
private fun Dropdown(
  value: String,
  items: List<String>,
  onChange: (String) -> Unit,
  label: String
) {
  var expanded by remember { mutableStateOf(false) }
  Column {
    Text(label, style = MaterialTheme.typography.bodySmall)
    Box {
      OutlinedButton(onClick = { expanded = true }) { Text(value) }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        items.forEach { item ->
          DropdownMenuItem(
            text = { Text(item) },
            onClick = { expanded = false; onChange(item) }
          )
        }
      }
    }
  }
}

private val httpClient by lazy { OkHttpClient() }

suspend fun triggerDispatch(
  token: String,
  runnerOwner: String,
  runnerRepo: String,
  payload: Map<String, Any?>
): Result<Unit> = withContext(Dispatchers.IO) {
  runCatching {
    val body = JSONObject()
      .put("event_type", "scaffold-nextjs")
      .put("client_payload", JSONObject().apply {
        payload.forEach { (k, v) -> put(k, v) }
      })
      .toString()

    val req = Request.Builder()
      .url("https://api.github.com/repos/$runnerOwner/$runnerRepo/dispatches")
      .addHeader("Accept", "application/vnd.github+json")
      .addHeader("Authorization", "Bearer $token")
      .addHeader("X-GitHub-Api-Version", "2022-11-28")
      .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
      .build()

    httpClient.newCall(req).execute().use { resp ->
      if (!resp.isSuccessful) {
        throw IllegalStateException("GitHub dispatch failed: HTTP ${resp.code} ${resp.message}")
      }
    }
  }
}
