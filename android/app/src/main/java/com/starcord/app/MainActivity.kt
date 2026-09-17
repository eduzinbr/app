package com.starcord.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.getcapacitor.BridgeActivity
import java.io.File

class MainActivity : BridgeActivity() {

    private val requiredPermissions: Array<String>
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            } else {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                criarPastaMidiaStarCord()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Injetando ponte JavaScript na WebView do Capacitor
        bridge.webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")

        // Solicita permissões e cria a pasta ao abrir o app
        verificarESolicitarPermissoes()
    }

    private fun verificarESolicitarPermissoes() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            criarPastaMidiaStarCord()
        }
    }

    private fun criarPastaMidiaStarCord() {
        // Cria a pasta: Android/data/com.starcord.app/files/midia/starcord/
        val mediaDir = File(getExternalFilesDir(null), "midia/starcord")
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }
    }

    class WebAppInterface(private val activity: MainActivity) {
        @JavascriptInterface
        fun requestOverlayPermission() {
            if (!Settings.canDrawOverlays(activity)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivity(intent)
            }
        }

        @JavascriptInterface
        fun startService() {
            if (Settings.canDrawOverlays(activity)) {
                val intent = Intent(activity, FloatingService::class.java)
                activity.startService(intent)
                activity.finishAffinity() // Sai do app e mantém o flutuante ativo
            }
        }
    }
}
