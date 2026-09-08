package com.js.tvremote.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onPrivacyClick: () -> Unit,
    onAdPrivacyClick: () -> Unit
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Acerca de") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
            }
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            Text("Js TV Remote", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Controla TVs compatibles desde tu teléfono mediante la red local WiFi.")

            Spacer(Modifier.height(20.dp))
            Text("Contacto / soporte:", fontWeight = FontWeight.Bold)
            TextButton(onClick = { openUrl("https://www.instagram.com/jostin.xxs") }) {
                Text("Instagram: @jostin.xxs")
            }

            Spacer(Modifier.height(16.dp))
            Text("Más herramientas gratis de Js:", fontWeight = FontWeight.Bold)
            TextButton(onClick = { openUrl("https://convertirpdf-doc-free-word-imagen-js.netlify.app") }) {
                Text("Convertir PDF, Word e imágenes")
            }
            TextButton(onClick = { openUrl("https://pdf-a-word-blue-convertir.netlify.app/") }) {
                Text("PDF a Word")
            }

            Spacer(Modifier.height(16.dp))
            Text("Privacidad", fontWeight = FontWeight.Bold)
            Text("La app controla dispositivos de tu red local y usa Google Mobile Ads para mostrar anuncios.")
            TextButton(onClick = onPrivacyClick) { Text("Ver política de privacidad") }
            TextButton(onClick = onAdPrivacyClick) { Text("Gestionar preferencias de anuncios") }
        }
    }
}
