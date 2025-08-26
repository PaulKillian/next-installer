package com.example.github.github

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.Executors

object GitHubClient {
    private val http = OkHttpClient.Builder().build()
    private val bg = Executors.newSingleThreadExecutor()

    fun createRepoAsync(
        token: String,
        name: String,
        description: String,
        privateRepo: Boolean,
        cb: (ok: Boolean, message: String) -> Unit
    ) {
        if (token.isBlank() || name.isBlank()) {
            cb(false, "Token and repo name are required.")
            return
        }

        bg.execute {
            try {
                val json = JSONObject().apply {
                    put("name", name)
                    put("description", description)
                    put("private", privateRepo)
                    put("auto_init", false)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("https://api.github.com/user/repos")
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer $token")
                    .post(body)
                    .build()

                http.newCall(req).execute().use { resp ->
                    val ok = resp.isSuccessful
                    val msg = if (ok) "Repo created: $name"
                              else "GitHub error ${resp.code}: ${resp.body?.string()}"
                    cb(ok, msg)
                }
            } catch (e: Exception) {
                cb(false, "Exception: ${e.message}")
            }
        }
    }
}
