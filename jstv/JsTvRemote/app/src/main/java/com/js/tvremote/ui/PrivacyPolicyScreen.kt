package com.js.tvremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Política de privacidad") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Js TV Remote", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Text("Última actualización: 4 de septiembre de 2026")
            Text("1. Función de la aplicación", fontWeight = FontWeight.Bold)
            Text("Js TV Remote permite descubrir y controlar televisores compatibles que están en la misma red local WiFi que el teléfono.")
            Text("2. Datos de red", fontWeight = FontWeight.Bold)
            Text("Para descubrir y controlar televisores, la app utiliza direcciones IP locales y protocolos de red como SSDP, HTTP y WebSocket. Estos datos se usan para ejecutar el control remoto y no se almacenan en un servidor propio de Js.")
            Text("3. Publicidad", fontWeight = FontWeight.Bold)
            Text("La app utiliza Google Mobile Ads (AdMob). El SDK puede recopilar y compartir dirección IP, interacciones con la app, información de diagnóstico y determinados identificadores del dispositivo para publicidad, análisis y prevención de fraude.")
            Text("4. Consentimiento", fontWeight = FontWeight.Bold)
            Text("Cuando corresponde, Google User Messaging Platform (UMP) gestiona las opciones de consentimiento y privacidad antes de solicitar anuncios.")
            Text("5. Servicios externos", fontWeight = FontWeight.Bold)
            Text("La app puede abrir sitios externos cuando el usuario selecciona enlaces de soporte o herramientas adicionales. Esos sitios tienen sus propias políticas de privacidad.")
            Text("6. Contacto", fontWeight = FontWeight.Bold)
            Text("Soporte: Instagram @jostin.xxs")
        }
    }
}
