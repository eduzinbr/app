package com.starcord.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
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
        
        // 1. Libera o acesso de Câmera e Microfone dentro da WebView do Capacitor
        bridge.webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                // Concede automaticamente a permissão solicitada pelo site/app web
                request?.grant(request.resources)
            }
        }

        // 2. Injeta a ponte JavaScript
        bridge.webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")

        // 3. Solicita as permissões nativas após um pequeno delay para garantir que a UI carregou
        bridge.webView.postDelayed({
            verificarESolicitarPermissoes()
        }, 1000)
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
                activity.finishAffinity()
            }
        }

        // Permite que o seu código JavaScript chame o pedido de permissões a qualquer momento
        @JavascriptInterface
        fun requestMediaPermissions() {
            activity.runOnUiThread {
                activity.verificarESolicitarPermissoes()
            }
        }
    }
}
