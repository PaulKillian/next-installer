package com.example.nextinstaller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Greeting("Android") }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name! This is Next Installer GUI placeholder.")
}
