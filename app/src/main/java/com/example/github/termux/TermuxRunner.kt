package com.example.github.termux

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object TermuxRunner {
    private const val TERMUX_PKG = "com.termux"
    private const val RUN_SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION = "com.termux.RUN_COMMAND"

    fun isInstalled(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo(TERMUX_PKG, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    fun openInstallPage(ctx: Context) {
        val url = "https://f-droid.org/packages/com.termux/"
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    /** Run a shell script inside Termux. */
    fun runCommand(
        ctx: Context,
        workdir: String = "/data/data/com.termux/files/home",
        script: String,
        background: Boolean = true,
        label: String = "next-installer"
    ) {
        val intent = Intent().apply {
            setClassName(TERMUX_PKG, RUN_SERVICE)
            action = ACTION
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", script))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", workdir)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", background)
            putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", label)
        }
        ctx.startService(intent)
    }
}
