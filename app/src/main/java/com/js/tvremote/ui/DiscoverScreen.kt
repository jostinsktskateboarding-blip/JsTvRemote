package com.js.tvremote.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.js.tvremote.net.TvDevice
import com.js.tvremote.net.SsdpDiscovery
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(
    onDeviceSelected: (TvDevice) -> Unit,
    onAboutClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<TvDevice>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    fun search() {
        scope.launch {
            searching = true
            devices = SsdpDiscovery.discover(context)
            searching = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { search() }

    fun startSearch() {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            search()
        }
    }

    LaunchedEffect(Unit) { startSearch() }

    Scaffold(
        containerColor = Color(0xFF0B0D10),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Js TV Remote", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Controla tu TV desde tu móvil", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                }
                IconButton(onClick = onAboutClick) {
                    Icon(Icons.Default.Info, contentDescription = "Acerca de")
                }
            }
        },
        bottomBar = { BannerAdView() }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15191F))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF173B38)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF5EEAD4))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Red local", fontWeight = FontWeight.SemiBold)
                        Text("Celular y TV deben estar en la misma WiFi", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tus televisores", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (searching) "Buscando dispositivos…" else "${devices.size} dispositivo(s) encontrado(s)",
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp
                    )
                }
                Button(onClick = { startSearch() }, enabled = !searching) {
                    Icon(if (searching) Icons.Default.Search else Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (searching) "Buscando" else "Buscar")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (searching) {
                Box(Modifier.fillMaxWidth().padding(34.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (devices.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF15191F))
                ) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(42.dp), tint = Color(0xFF5EEAD4))
                        Spacer(Modifier.height(10.dp))
                        Text("No encontramos tu TV", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Enciende el televisor y comprueba que ambos dispositivos estén conectados a la misma red WiFi.",
                            color = Color(0xFF9CA3AF),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(devices, key = { "${it.ip}-${it.brand}" }) { device ->
                    TvDeviceCard(device = device, onClick = { onDeviceSelected(device) })
                }
            }
        }
    }
}

@Composable
private fun TvDeviceCard(device: TvDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15191F))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF20262E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFFE5E7EB))
            }
            Column(Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.SemiBold)
                Text("${device.ip} · ${brandLabel(device)}", color = Color(0xFF9CA3AF), fontSize = 12.sp)
            }
            TextButton(onClick = onClick) { Text("Abrir") }
        }
    }
}

private fun brandLabel(device: TvDevice): String = when (device.brand) {
    com.js.tvremote.net.Brand.ANDROID_TV -> "Android TV / Google TV"
    com.js.tvremote.net.Brand.ROKU -> "Roku"
    com.js.tvremote.net.Brand.LG_WEBOS -> "LG webOS"
    com.js.tvremote.net.Brand.SAMSUNG -> "Samsung"
    com.js.tvremote.net.Brand.GENERIC_DLNA -> "DLNA"
    com.js.tvremote.net.Brand.UNKNOWN -> "Dispositivo"
}
