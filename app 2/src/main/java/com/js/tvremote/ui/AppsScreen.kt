package com.js.tvremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.js.tvremote.net.TvDevice
import com.js.tvremote.ui.theme.TvRemoteTextPrimary
import com.js.tvremote.ui.theme.TvRemoteTextSecondary

@Composable
fun AppsScreen(
    device: TvDevice,
    onSelectRemoto: () -> Unit,
    onSelectAjustes: () -> Unit
) {
    Scaffold(containerColor = Color.Black) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Apps",
                color = TvRemoteTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            Text(
                "El lanzador de apps de ${device.name} llegará en una próxima actualización.",
                color = TvRemoteTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.weight(1f))

            RemoteBottomNav(
                selected = RemoteTab.APPS,
                onSelectRemoto = onSelectRemoto,
                onSelectApps = {},
                onSelectAjustes = onSelectAjustes
            )
        }
    }
}
